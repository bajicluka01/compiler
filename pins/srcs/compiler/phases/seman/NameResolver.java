/**
 * @author sliva
 */
package compiler.phases.seman;

import compiler.common.report.*;
import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;

/**
 * Name resolving: the result is stored in {@link SemAn#declaredAt}.
 * 
 * @author sliva
 */
public class NameResolver extends AbsFullVisitor<Object, Object> {

	/** Symbol table. */
	private final SymbTable symbTable = new SymbTable();

	public Object visit(AbsSource source, Object visArg) {
		//this.visit(source, visArg);
		AbsDecls decls = source.decls;
		//for(int i = 0; i < decls.numDecls(); i++)
		decls.accept(this, decls.decl(0));
		return visArg;
	}

	public Object visit(AbsParDecls decls, Object visArg) {
		for (AbsDecl decl : decls.parDecls()) {
			try {
				symbTable.ins(decl.name, decl);
			} catch (Exception e) {
				throw new Report.Error(decl.location(), "Name '" + decl.name + "' already declared!");
			}
		}
		for (AbsDecl decl : decls.parDecls()) {
			symbTable.newScope();
			decl.accept(this, visArg);

			symbTable.oldScope();
		}
		return null;
	}

	public Object visit(AbsDecls decls, Object visArg) {
		for (AbsDecl decl : decls.decls()) {
			try {
				symbTable.ins(decl.name, decl);
			} catch (Exception e) {
				throw new Report.Error(decl.location(), "Name '" + decl.name + "' already declared!");
			}
		}
		for (AbsDecl decl : decls.decls()) {
			symbTable.newScope();

			decl.accept(this, visArg);
			try {
				SemAn.declaredAt.put(new AbsVarName(new Location(decl), decl.name), decl);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			symbTable.oldScope();
		}
		return visArg;
	}

	public Object visit(AbsDecl decl, Object visArg) {
		try {
			SemAn.declaredAt.put(new AbsVarName(new Location(decl), decl.name), symbTable.fnd(decl.name));
		}
		catch(Exception e) {

		}
		return visArg;
	}

	public Object visit(AbsVarName var, Object visArg) {
		try {
		SemAn.declaredAt.put(var, symbTable.fnd(var.name));
		}
		catch(Exception e) {
			throw new Report.Error(var.location(), "Name '" + var.name + "' not defined in current scope!");
		}
		return visArg;
	}

	public Object visit(AbsTypName typ, Object visArg) {
		try {
			SemAn.declaredAt.put(typ, symbTable.fnd(typ.name));
		}
		catch(Exception e) {
			throw new Report.Error(typ.location(), "Name '" + typ.name + "' not defined in current scope!");
		}
		return visArg;
	}

	public Object visit(AbsFunName funName, Object visArg) {
		try{
			SemAn.declaredAt.put(funName, symbTable.fnd(funName.name));
		}catch(Exception e) {
			throw new Report.Error(funName.location(), "Name '" + funName.name + "'not defined in current scope!");
		}
		return visArg;
	}

}
