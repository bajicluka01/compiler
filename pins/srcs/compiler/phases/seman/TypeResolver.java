/**
 * @author sliva 
 */
package compiler.phases.seman;

import compiler.common.report.*;
import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.data.type.*;
import compiler.data.type.property.*;

/**
 * Type resolving: the result is stored in {@link SemAn#declaresType},
 * {@link SemAn#isType}, and {@link SemAn#isOfType}.
 * 
 * @author sliva
 */
public class TypeResolver extends AbsFullVisitor<SemType, TypeResolver.Phase> {

	/**
	 * Different phases of type resolving.
	 * 
	 * @author sliva
	 */
	protected static enum Phase {

		/** Type checking of expressions. */
		CHECK,

		/**
		 * The phase for declaring type names. Every type declaration in the current
		 * declaration group is associated with its own empty {@link SemNamedType}.
		 */
		TYP_DECLARE,

		/**
		 * The phase for defining types (after {@link TYP_DECLARE}). Every
		 * {@link SemNamedType} of a type declaration in the current declaration group
		 * is assigned its type.
		 */
		TYP_DEFINE,

		/**
		 * The phase for type checking of types (after {@link TYP_DEFINE}). Every type
		 * represented by {@link SemNamedType} of a type declaration in the current
		 * declaration group is checked.
		 */
		TYP_CHECK,

		/**
		 * The phase for typing and type checking (after {@link TYP_CHECK}). A type of
		 * every variable in the current declaration group is defined and checked.
		 */
		VAR,

		/**
		 * The phase for typing functions (after {@link VAR}. Types of parameters and
		 * the type of function result of every function in the current declaration
		 * group are defined.
		 */
		FUN_DEFINE,

		/**
		 * The phase for type checking functions (after {@link FUN_DEFINE}). Types of
		 * parameters and the type of function result of every function in the current
		 * declaration group are checked. Furthermore, the return value of a function is
		 * typed and type checked.
		 */
		FUN_CHECK,
	};

	public SemType visit(AbsSource source, TypeResolver.Phase visArg) {
		super.visit(source, Phase.TYP_DECLARE);
		super.visit(source, Phase.TYP_DEFINE);
		//super.visit(source, Phase.TYP_CHECK);
		super.visit(source, Phase.VAR);
		super.visit(source, Phase.FUN_DEFINE);
		super.visit(source, Phase.FUN_CHECK);
		//super.visit(source, Phase.CHECK);
		return null;
	}


	public SemType visit(AbsTypDecl decl, TypeResolver.Phase visArg) {
		switch(visArg) {
			case TYP_DECLARE: {
				SemAn.declaresType.put(decl, new SemNamedType(decl.name));
				break;
			}
			case TYP_DEFINE: {
				SemType t = decl.type.accept(this, visArg);
				SemAn.declaresType.get(decl).define(t);
				break;
			}
			case TYP_CHECK: {
				SemType t = decl.type.accept(this, visArg);
				if(t.isInfinite())
					throw new Report.Error(decl.location(), "Infinite type declaration!");
			}
		}
		return null;
	}

	public SemType visit(AbsTypName typName, TypeResolver.Phase visArg) {
		switch(visArg) {
			case VAR:
			case TYP_DEFINE: {
				AbsDecl decl = SemAn.declaredAt.get(typName);
				//if(decl instanceof AbsTypDecl) {
					SemType type = SemAn.declaresType.get((AbsTypDecl)decl);
					SemAn.isType.put(typName, type.actualType());
					return type;
				//}
				//break;
			}

			case TYP_CHECK: {
				return SemAn.isType.get(typName);
			}
		}
		return null;
	}

	public SemType visit(AbsVarDecl decl, TypeResolver.Phase visArg) {
		switch(visArg) {

			case TYP_DEFINE:
			case TYP_CHECK: {
				decl.type.accept(this, visArg);
				break;
			}

			case VAR: {
				SemType t = decl.type.accept(this, visArg);
				if (t.matches(new SemVoidType()))
					throw new Report.Error(decl.location(), "Invalid void declaration!");
			}
		}
		return null;
	}

	public SemType visit(AbsParDecls parDecls, TypeResolver.Phase visArg) {
		switch(visArg) {
			case TYP_DECLARE:
			case VAR:
			case TYP_DEFINE:
			case FUN_DEFINE: {
				for(AbsParDecl decl : parDecls.parDecls()) {
					decl.accept(this, visArg);
				}
				break;
			}
		}

		return null;
	}

	public SemType visit(AbsParDecl parDecl, TypeResolver.Phase visArg) {
		switch(visArg) {
			case TYP_DEFINE: {
				parDecl.type.accept(this, visArg);
				break;
			}
			case TYP_DECLARE: {
				parDecl.type.accept(this, visArg);
				break;
			}
			case VAR: {
				SemType t = parDecl.type.accept(this, visArg);
				if (t.matches(new SemVoidType()))
					throw new Report.Error(parDecl.location(), "Invalid void declaration!");
				return t;
			}
		}
		return null;
	}

	public SemType visit(AbsWhileStmt whileStmt, TypeResolver.Phase visArg) {
		switch(visArg) {
			case FUN_CHECK: {
				for(AbsStmt stmt : whileStmt.stmts.stmts()) {
					//if()
				}
				break;
			}
			case TYP_DEFINE: {
				whileStmt.cond.accept(this, visArg);
				whileStmt.stmts.accept(this, visArg);
			}
		}
		return null;
	}

	public SemType visit(AbsVarName varName, TypeResolver.Phase visArg) {
		switch(visArg) {
			case FUN_CHECK: {
				AbsDecl decl = SemAn.declaredAt.get(varName);
				SemType t = SemAn.isType.get(decl.type);
				SemAn.isOfType.put(varName, t);
				return t;
			}
		}
		return null;
	}

	public SemType visit(AbsArrExpr arrExpr, TypeResolver.Phase visArg) {
		switch(visArg) {
			case TYP_CHECK:
			case VAR:
			case FUN_DEFINE:
			case TYP_DEFINE: {
				SemType arr = arrExpr.array.accept(this, visArg);
				SemType type = ((SemArrType) arr).elemType;
				SemAn.isOfType.put(arrExpr, type);
				return type;
			}
		}
		return null;
	}

	public SemType visit(AbsPtrType ptrType, TypeResolver.Phase visArg) {
		switch(visArg) {
			case TYP_CHECK: {

			}
		}
		return null;
	}

	public SemType visit(AbsExprStmt exprStmt, TypeResolver.Phase visArg) {
		exprStmt.expr.accept(this, visArg);
		return null;
	}

	public SemType visit(AbsStmts stmts, TypeResolver.Phase visArg) {
		return null;
	}

	public SemType visit(AbsBlockExpr blockExpr, TypeResolver.Phase visArg) {
		return null;
	}

	public SemType visit(AbsAtomType atom, TypeResolver.Phase visArg) {
		switch(visArg) {
			case TYP_CHECK:
			case VAR:
			case FUN_DEFINE:
			case TYP_DEFINE: {
				switch(atom.type) {
					case INT: {
						SemType t = new SemIntType();
						if(SemAn.isType.get(atom) == null)
							SemAn.isType.put(atom, t);
						return t;
					}
					case CHAR: {
						SemType t = new SemCharType();
						if(SemAn.isType.get(atom) == null)
							SemAn.isType.put(atom, t);
						return t;
					}
					case BOOL: {
						SemType t = new SemBoolType();
						if(SemAn.isType.get(atom) == null)
							SemAn.isType.put(atom, t);
						return t;
					}
					case VOID: {
						SemType t = new SemVoidType();
						if(SemAn.isType.get(atom) == null)
							SemAn.isType.put(atom, t);
						return t;
					}
					default: {
						throw new Report.Error(atom.location(), "Atom type not recognised!");
					}
				}
			}
		}
		return null;
	}

	public SemType visit(AbsFunDef funDef, TypeResolver.Phase visArg) {
		switch(visArg) {
			case TYP_DEFINE: {
				funDef.parDecls.accept(this, visArg);
				break;
			}
			case TYP_DECLARE: {
				funDef.parDecls.accept(this, visArg);
				break;
			}
			case FUN_DEFINE: {
				funDef.parDecls.accept(this, visArg);
			}
			case VAR: {
				funDef.parDecls.accept(this, visArg);
			}
		}

		return null;
	}

}
