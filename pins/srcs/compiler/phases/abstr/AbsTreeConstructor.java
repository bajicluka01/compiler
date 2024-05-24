/**
 * @author sliva
 */
package compiler.phases.abstr;

import java.util.*;
import compiler.common.report.*;
import compiler.data.dertree.*;
import compiler.data.dertree.visitor.*;
import compiler.data.abstree.*;
import compiler.data.symbol.Symbol;

/**
 * Transforms a derivation tree to an abstract syntax tree.
 *
 * @author sliva
 */
public class AbsTreeConstructor implements DerVisitor<AbsTree, AbsTree> {

	@Override
	public AbsTree visit(DerLeaf leaf, AbsTree visArg) {
		throw new Report.InternalError();
	}

	@Override
	public AbsTree visit(DerNode node, AbsTree visArg) {
		//System.out.println(node.label);
		switch (node.label) {
			case Source: {
				//AbsDecls decls = (AbsDecls) node.subtree(0).accept(this, null);
				Vector<AbsDecl> allDecls = new Vector<AbsDecl>();
				AbsDecl decl = (AbsDecl) node.subtree(0).accept(this, null);
				allDecls.add(decl);
				AbsDecls decls = (AbsDecls) node.subtree(1).accept(this, null);
				if (decls != null)
					allDecls.addAll(decls.decls());
				//return new AbsDecls(new Location(decl, decls == null ? decl : decls), allDecls);
				if(decls == null)
					return new AbsSource(new Location(0,0,0,0), new AbsDecls(new Location(decl, decls == null ? decl : decls), allDecls));
				return new AbsSource(decls.location(), new AbsDecls(new Location(decl, decls == null ? decl : decls), allDecls));
			}

			case Decls:
			case DeclsRest: {
				if (node.numSubtrees() == 0)
					return null;
				Vector<AbsDecl> allDecls = new Vector<AbsDecl>();
				AbsDecl decl = (AbsDecl) node.subtree(0).accept(this, null);
				allDecls.add(decl);
				AbsDecls decls = (AbsDecls) node.subtree(1).accept(this, null);
				if (decls != null)
					allDecls.addAll(decls.decls());
				return new AbsDecls(new Location(decl, decls == null ? decl : decls), allDecls);
			}

			case Decl: {
				switch(((DerLeaf)node.subtree(0)).symb.token) {
					case VAR: {
						AbsType t = (AbsType)node.subtree(3).accept(this, null);
						return new AbsVarDecl(((DerLeaf)node.subtree(0)).symb.location(), ((DerLeaf)node.subtree(1)).symb.lexeme, t);
					}
					case TYP: {
						AbsType t = (AbsType)node.subtree(3).accept(this, null);
						return new AbsTypDecl(((DerLeaf)node.subtree(0)).symb.location(), ((DerLeaf)node.subtree(1)).symb.lexeme, t);
					}
					case FUN: {
						AbsParDecls par = (AbsParDecls)node.subtree(3).accept(this, null);
						if(par == null) {
							par = new AbsParDecls(new Location(0,0), new Vector<>());
						}
						AbsType type = (AbsType)node.subtree(6).accept(this, null);
						AbsBlockExpr block = (AbsBlockExpr)node.subtree(7).accept(this, null);
						if(block == null)
							return new AbsFunDecl(new Location(node, type), ((DerLeaf)node.subtree(1)).symb.lexeme, par, type);
						else
							return new AbsFunDef(new Location(node, block), ((DerLeaf)node.subtree(1)).symb.lexeme, par, type, block);
					}
				}
			}

			case FunStmts: {
				if(node.numSubtrees() == 0)
					return visArg;
				else {
					AbsStmts stmts = (AbsStmts)node.subtree(1).accept(this, null);
					AbsExpr expr = (AbsExpr)node.subtree(3).accept(this, null);
					AbsDecls decls = (AbsDecls)node.subtree(4).accept(this, null);
					if(decls == null)
						decls = new AbsDecls(new Location(0,0), new Vector<>());
					return new AbsBlockExpr(new Location(node), decls, stmts, expr);
				}
			}

			case WhereStmt: {
				if(node.numSubtrees() == 0)
					return visArg;
				else {
					Vector<AbsDecl> allDecls = new Vector<AbsDecl>();
					AbsDecl decl = (AbsDecl) node.subtree(2).accept(this, null);
					allDecls.add(decl);
					AbsDecls decls = (AbsDecls) node.subtree(3).accept(this, null);
					if (decls != null)
						allDecls.addAll(decls.decls());
					return new AbsDecls(new Location(decl, decls == null ? decl : decls), allDecls);
				}
			}

			case FunParams: {
				if(node.numSubtrees() == 0)
					return visArg;
				else {
					Vector<AbsParDecl> v = new Vector<>();
					String name = ((DerLeaf)node.subtree(0)).symb.lexeme;
					AbsType type = (AbsType)node.subtree(2).accept(this, null);
					AbsParDecl par = new AbsParDecl(new Location(node.subtree(0), type), name, type);
					v.add(par);
					AbsParDecls decls = (AbsParDecls)node.subtree(3).accept(this, null);
					if(decls != null)
						v.addAll(decls.parDecls());
					return new AbsParDecls(new Location(par, decls == null ? par : decls), v);
				}
			}

			case FunParamsRest: {
				if(node.numSubtrees() == 0)
					return visArg;
				else {
					Vector<AbsParDecl> v = new Vector<>();
					String name = ((DerLeaf)node.subtree(1)).symb.lexeme;
					AbsType type = (AbsType)node.subtree(3).accept(this, null);
					AbsParDecl par = new AbsParDecl(new Location(node.subtree(3), type), name, type);
					v.add(par);
					AbsParDecls decls = (AbsParDecls)node.subtree(4).accept(this, null);
					if(decls != null)
						v.addAll(decls.parDecls());
					return new AbsParDecls(new Location(par, decls == null ? par : decls), v);
				}
			}

			case Type: {
				switch(((DerLeaf)node.subtree(0)).symb.token) {
					case BOOL: {
						return new AbsAtomType(((DerLeaf)node.subtree(0)).symb.location(), AbsAtomType.Type.BOOL);
					}
					case VOID: {
						return new AbsAtomType(((DerLeaf)node.subtree(0)).symb.location(), AbsAtomType.Type.VOID);
					}
					case CHAR: {
						return new AbsAtomType(((DerLeaf)node.subtree(0)).symb.location(), AbsAtomType.Type.CHAR);
					}
					case INT: {
						return new AbsAtomType(((DerLeaf)node.subtree(0)).symb.location(), AbsAtomType.Type.INT);
					}
					case PTR: {
						AbsType t = (AbsType)node.subtree(1).accept(this, null);
						return new AbsPtrType(((DerLeaf)node.subtree(0)).symb.location(), t);
					}
					case IDENTIFIER: {
						return new AbsTypName(((DerLeaf)node.subtree(0)).symb.location(), ((DerLeaf)node.subtree(0)).symb.toString());
					}
					case ARR: {
						AbsExpr e = (AbsExpr)node.subtree(2).accept(this, null);
						AbsType t = (AbsType)node.subtree(4).accept(this, null);
						return new AbsArrType(new Location(node), e, t);
					}
				}
			}

			case Expr: {
				AbsExpr e = (AbsExpr)node.subtree(0).accept(this, null);
				return e;
			}

			case RelExpr: {
				AbsExpr e = (AbsExpr)node.subtree(0).accept(this, null);
				return node.subtree(1).accept(this, e);
			}

			case AddExprRest: {
				if(node.numSubtrees() == 0)
					return visArg;

				/*Location location = new Location(node, node);
				AbsBinExpr operator = (AbsBinExpr) node.subtree(0).accept(this, null);
				AbsExpr expr1 = (AbsExpr) visArg;
				AbsExpr expr2 = (AbsExpr) node.subtree(1).accept(this, null);

				return new AbsBinExpr(location, operator.oper, expr1, expr2);*/

				AbsExpr e = (AbsExpr)visArg;
				AbsExpr e2 = (AbsExpr)node.subtree(1).accept(this, null);

				switch(((DerLeaf)node.subtree(0)).symb.token) {
					case ADD: {
						return new AbsBinExpr(new Location(node), AbsBinExpr.Oper.ADD, e, e2);
					}
					case SUB: {
						return new AbsBinExpr(new Location(node), AbsBinExpr.Oper.SUB, e, e2);
					}
				}
			}

			case MulExprRest: {
				if(node.numSubtrees() == 0)
					return visArg;

				/*Location location = new Location(node, node);
				AbsBinExpr operator = (AbsBinExpr) node.subtree(0).accept(this, null);
				AbsExpr expr1 = (AbsExpr) visArg;
				AbsExpr expr2 = (AbsExpr) node.subtree(1).accept(this, null);

				return new AbsBinExpr(location, operator.oper, expr1, expr2);*/

				AbsExpr e = (AbsExpr)visArg;
				AbsExpr e2 = (AbsExpr)node.subtree(1).accept(this, null);

				switch(((DerLeaf)node.subtree(0)).symb.token) {
					case MUL: {
						return new AbsBinExpr(new Location(node), AbsBinExpr.Oper.MUL, e, e2);
					}
					case DIV: {
						return new AbsBinExpr(new Location(node), AbsBinExpr.Oper.DIV, e, e2);
					}
					case MOD: {
						return new AbsBinExpr(new Location(node), AbsBinExpr.Oper.MOD, e, e2);
					}
				}

			}
			case PstfExprRest: {

				//System.out.println((visArg == null) + " " + node.numSubtrees());
				if(node.numSubtrees() == 0)
					return visArg;

				/*Location location = new Location(node, node);
				AbsBinExpr operator = (AbsBinExpr) node.subtree(0).accept(this, null);
				AbsExpr expr1 = (AbsExpr) visArg;
				AbsExpr expr2 = (AbsExpr) node.subtree(1).accept(this, null);

				return new AbsBinExpr(location, operator.oper, expr1, expr2);*/

				/*System.out.println(visArg == null);

				AbsExpr expr1 = (AbsExpr) node.subtree(1).accept(this, null);
				AbsExpr expr2 = (AbsExpr) node.subtree(3).accept(this, null);
				if(expr2 != visArg)
					return new AbsArrExpr(new Location(node), expr1, expr2);
				return new AbsArrExpr(new Location(node), (AbsExpr)visArg != null ? (AbsExpr)visArg : expr1, expr1);*/

				AbsExpr e = (AbsExpr)visArg;
				AbsExpr e2 = (AbsExpr)node.subtree(1).accept(this, null);

				return new AbsArrExpr(new Location(node), e, e2);
			}
			case RelExprRest: {
				if(node.numSubtrees() == 0)
					return visArg;

				/*Location location = new Location(node, node);
				AbsBinExpr operator = (AbsBinExpr) node.subtree(0).accept(this, null);
				AbsExpr expr1 = (AbsExpr) visArg;
				AbsExpr expr2 = (AbsExpr) node.subtree(1).accept(this, expr1);

				return new AbsBinExpr()*/

				AbsExpr e = (AbsExpr)visArg;
				AbsExpr e2 = (AbsExpr)node.subtree(1).accept(this, null);

				switch(((DerLeaf)node.subtree(0)).symb.token) {
					case GTH: {
						return new AbsBinExpr(new Location(node), AbsBinExpr.Oper.GTH, e, e2);
					}
					case ADD: {
						return new AbsBinExpr(new Location(node), AbsBinExpr.Oper.ADD, e, e2);
					}
					case SUB: {
						return new AbsBinExpr(new Location(node), AbsBinExpr.Oper.SUB, e, e2);
					}
					case DIV: {
						return new AbsBinExpr(new Location(node), AbsBinExpr.Oper.DIV, e, e2);
					}
					case LTH: {
						return new AbsBinExpr(new Location(node), AbsBinExpr.Oper.LTH, e, e2);
					}
					case MUL: {
						return new AbsBinExpr(new Location(node), AbsBinExpr.Oper.MUL, e, e2);
					}
					case MOD: {
						return new AbsBinExpr(new Location(node), AbsBinExpr.Oper.MOD, e, e2);
					}
					case LEQ: {
						return new AbsBinExpr(new Location(node), AbsBinExpr.Oper.LEQ, e, e2);
					}
					case GEQ: {
						return new AbsBinExpr(new Location(node), AbsBinExpr.Oper.GEQ, e, e2);
					}
					case EQU: {
						return new AbsBinExpr(new Location(node), AbsBinExpr.Oper.EQU, e, e2);
					}
					case NEQ: {
						return new AbsBinExpr(new Location(node), AbsBinExpr.Oper.NEQ, e, e2);
					}
				}
			}

			case Unop: {
				switch(((DerLeaf)node.subtree(0)).symb.token) {
					case ADD: {
						return new AbsUnExpr(new Location(node), AbsUnExpr.Oper.ADD, (AbsExpr)visArg);
					}
					case SUB: {
						return new AbsUnExpr(new Location(node), AbsUnExpr.Oper.SUB, (AbsExpr)visArg);
					}
					case DATA: {
						return new AbsUnExpr(new Location(node), AbsUnExpr.Oper.DATA, (AbsExpr)visArg);
					}
					case ADDR: {
						return new AbsUnExpr(new Location(node), AbsUnExpr.Oper.ADDR, (AbsExpr)visArg);
					}
				}
			}

			case AddExpr: {
				AbsExpr e = (AbsExpr)node.subtree(0).accept(this, null);
				return node.subtree(1).accept(this, e);
			}

			case MulExpr: {
				AbsExpr e = (AbsExpr)node.subtree(0).accept(this, null);
				return node.subtree(1).accept(this, e);
			}

			case PrefExpr: {
				AbsExpr e = (AbsExpr)node.subtree(0).accept(this, null);
				return e;
			}

			case PstfExpr: {
				AbsExpr e = (AbsExpr)node.subtree(0).accept(this, null);
				return node.subtree(1).accept(this, e);
			}

			case AtomExpr: {
				if(node.numSubtrees() == 1) {
					switch (((DerLeaf) node.subtree(0)).symb.token) {
						case INTCONST:
							return new AbsAtomExpr(new Location(node), AbsAtomExpr.Type.INT, ((DerLeaf) node.subtree(0)).symb.lexeme);
						case BOOLCONST:
							return new AbsAtomExpr(new Location(node), AbsAtomExpr.Type.BOOL, ((DerLeaf) node.subtree(0)).symb.lexeme);
						case CHARCONST:
							return new AbsAtomExpr(new Location(node), AbsAtomExpr.Type.CHAR, ((DerLeaf) node.subtree(0)).symb.lexeme);
						case VOIDCONST:
							return new AbsAtomExpr(new Location(node), AbsAtomExpr.Type.VOID, ((DerLeaf) node.subtree(0)).symb.lexeme);
						case PTRCONST:
							return new AbsAtomExpr(new Location(node), AbsAtomExpr.Type.PTR, ((DerLeaf) node.subtree(0)).symb.lexeme);
						case IDENTIFIER:
							return new AbsVarName(new Location(node), ((DerLeaf) node.subtree(0)).symb.lexeme);
					}
				}
				if(node.numSubtrees() == 2) {
					switch (((DerLeaf) node.subtree(0)).symb.token) {
						case IDENTIFIER:
							AbsVarName name = new AbsVarName(new Location(node), ((DerLeaf) node.subtree(0)).symb.lexeme);
							break;
						default:
							throw new Report.Error(new Location(node), ((DerLeaf) node.subtree(0)).symb.lexeme + " is not callable!");
					}
					AbsArgs args = (AbsArgs)node.subtree(1).accept(this, null);
					if(args == null)
						return new AbsVarName(new Location(node), ((DerLeaf) node.subtree(0)).symb.lexeme);
					return new AbsFunName(new Location(node), ((DerLeaf) node.subtree(0)).symb.lexeme, args);
				}
			}

			case CallExpr: {
				if(node.numSubtrees() == 0)
					//return new AbsArgs(new Location(0,0), new Vector<>());
					return null;
				AbsArgs args = (AbsArgs)node.subtree(1).accept(this, null);
				return args;
			}

			case CallParams: {
				if(node.numSubtrees() == 0)
					return new AbsArgs(new Location(0,0), new Vector<>());
				else {
					Vector<AbsExpr> v = new Vector<>();
					AbsExpr arg = (AbsExpr)node.subtree(0).accept(this, null);
					v.add(arg);
					AbsArgs args2 = (AbsArgs) node.subtree(1).accept(this, null);
					if (args2 != null)
						v.addAll(args2.args());
					//return new AbsStmts(new Location(stmt, stmts == null ? stmt : stmts), v);
					return new AbsArgs(new Location(node), v);
				}
			}

			case CallParamsRest: {
				if(node.numSubtrees() == 0)
					return new AbsArgs(new Location(0,0), new Vector<>());
				else {
					Vector<AbsExpr> v = new Vector<>();
					AbsExpr arg = (AbsExpr)node.subtree(1).accept(this, null);
					v.add(arg);
					AbsArgs args2 = (AbsArgs) node.subtree(2).accept(this, null);
					if (args2 != null)
						v.addAll(args2.args());
					//return new AbsStmts(new Location(stmt, stmts == null ? stmt : stmts), v);
					return new AbsArgs(new Location(node), v);
				}
			}

			case Stmt: {
				if(node.subtree(0) instanceof DerLeaf) {
					switch(((DerLeaf)node.subtree(0)).symb.token) {
						case IF: {
							AbsExpr condition = (AbsExpr)node.subtree(1).accept(this, null);
							AbsStmts thenStmts = (AbsStmts)node.subtree(3).accept(this, null);
							AbsStmts elseStmts = (AbsStmts)node.subtree(4).accept(this, null);
							if(elseStmts == null)
								elseStmts = new AbsStmts(new Location(0,0), new Vector<>());
							return new AbsIfStmt(new Location(node), condition, thenStmts, elseStmts);
						}
						case WHILE: {
							AbsExpr condition = (AbsExpr)node.subtree(1).accept(this, null);
							AbsStmts whileStmts = (AbsStmts)node.subtree(3).accept(this, null);
							return new AbsWhileStmt(new Location(node), condition, whileStmts);
						}
					}
				}
				else {
					AbsExpr expr = (AbsExpr)node.subtree(0).accept(this, null);
					AbsExpr expr2 = (AbsExpr)node.subtree(1).accept(this, null);  //ExprRest
					if(expr2 == null)
						return new AbsExprStmt(new Location(node), expr);
					else
						return new AbsAssignStmt(new Location(node), expr, expr2);
				}
			}

			case Stmts: {
				if (node.numSubtrees() == 0)
					return visArg;
				else {
					Vector<AbsStmt> v = new Vector<>();
					AbsStmt stmt = (AbsStmt)node.subtree(0).accept(this, null);
					v.add(stmt);
					AbsStmts stmts = (AbsStmts) node.subtree(1).accept(this, null);
					if (stmts != null)
						v.addAll(stmts.stmts());
					return new AbsStmts(new Location(stmt, stmts == null ? stmt : stmts), v);
				}
			}

			case StmtsRest: {
				if(node.numSubtrees() == 0)
					return visArg;
				else {
					Vector<AbsStmt> v = new Vector<>();
					AbsStmt stmt = (AbsStmt)node.subtree(1).accept(this, null);
					v.add(stmt);
					AbsStmts stmts = (AbsStmts)node.subtree(2).accept(this, null);
					if(stmts != null)
						v.addAll(stmts.stmts());
					return new AbsStmts(new Location(stmt, stmts == null ? stmt : stmts), v);
				}
			}

			case AssignStmt: {
				if(node.numSubtrees() == 0)
					return visArg;
				AbsExpr e = (AbsExpr)node.subtree(1).accept(this, null);
				return e;
			}

			case ElseStmt: {
				if(node.numSubtrees() == 1)  //samo "end"
					return null;
				else {
					AbsStmts elseStmts = (AbsStmts)node.subtree(1).accept(this, null);
					return elseStmts;
				}
			}

		}
		return new AbsDecls(new Location(0,0,0,0), new Vector<>());
		//return null;
	}
}