/**
 * @author sliva
 */
package compiler.phases.frames;

import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.data.type.*;
import compiler.data.layout.*;
import compiler.phases.seman.*;

/**
 * Computing function frames and accesses.
 * 
 * @author sliva
 */
public class FrmEvaluator extends AbsFullVisitor<Object, FrmEvaluator.Context> {

	/**
	 * The context {@link FrmEvaluator} uses while computing function frames and
	 * variable accesses.
	 * 
	 * @author sliva
	 */
	protected abstract class Context {
	}

	/**
	 * Functional context, i.e., used when traversing function and building a new
	 * frame, parameter acceses and variable acceses.
	 * 
	 * @author sliva
	 */
	private class FunContext extends Context {
		public int depth = 0;
		public long locsSize = 0;
		public long argsSize = 0;
		public long parsSize = new SemPtrType(new SemVoidType()).size();
	}

    public Object visit(AbsSource source, Context visArg) {
		FunContext context = new FunContext();
		context.depth = 0;
		return super.visit(source, context);
	}

	public Object visit(AbsVarDecl varDecl, Context visArg) {
		SemType type = SemAn.isType.get(varDecl.type);
		FunContext context = (FunContext) visArg;
		if(context.depth == 0) {
			AbsAccess access = new AbsAccess(type.size(), new Label(varDecl.name));
			Frames.accesses.put(varDecl, access);
			return null;
		}
		else if(type != null) {
			context.locsSize += type.size();
			RelAccess access = new RelAccess(type.size(), context.locsSize, context.depth);
			Frames.accesses.put(varDecl, access);
		}
		return super.visit(varDecl, visArg);
	}

	public Object visit(AbsFunDef funDef, Context visArg) {
		FunContext context = new FunContext();
		context.argsSize += new SemPtrType(new SemVoidType()).size();
		context.depth = ((FunContext)visArg).depth + 1;
		super.visit(funDef, context);
		Label label;
		if(context.depth == 1)
			label = new Label(funDef.name);
		else
			label = new Label();
		Frames.frames.put(funDef, new Frame(label, ((FunContext)visArg).depth + 1, context.locsSize, context.argsSize));
		return null;
	}

	public Object visit(AbsFunDecl funDecl, Context visArg) {
		FunContext context = (FunContext) visArg;
		Label label;
		if(context.depth == 0)
			label = new Label(funDecl.name);
		else
			label = new Label();
		context.argsSize += new SemPtrType(new SemVoidType()).size();
		Frames.frames.put(funDecl, new Frame(label, ((FunContext)visArg).depth + 1, 0, 0));
		return null;
	}

	public Object visit(AbsParDecl parDecl, Context visArg) {
		SemType type = SemAn.isType.get(parDecl.type);
		FunContext context = (FunContext) visArg;
		if(type != null) {
			//AbsAccess access = new AbsAccess(type.size(), new Label(parDecl.name));
			RelAccess access = new RelAccess(type.size(), context.parsSize, context.depth);
			context.parsSize += type.size();
			Frames.accesses.put(parDecl, access);
		}
		return null;
	}

	//function call
	public Object visit(AbsFunName funName, Context visArg) {
		FunContext context = (FunContext) visArg;
		long s = (long)funName.args.accept(this, visArg);
		context.argsSize = s > context.argsSize ? s : context.argsSize;
		return null;
	}

	public Object visit(AbsArgs args, Context visArg) {
		long totalSize = new SemPtrType(new SemVoidType()).size(); //Static Link
		for(AbsExpr expr : args.args()) {
			//expr.accept(this, visArg);
			//totalSize += SemAn.isOfType.get(expr).size(); //needs fix
			totalSize += 8;
		}
		return totalSize;
	}

}
