/**
 * @author sliva
 */
package compiler.phases.seman;

import compiler.common.report.*;
import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.data.type.SemPtrType;

/**
 * Determines which value expression can denote an address.
 * 
 * @author sliva
 */
public class AddrResolver extends AbsFullVisitor<Boolean, Object> {

    public Boolean visit(AbsAtomExpr atomExpr, Object visArg) {
        SemAn.isAddr.put(atomExpr, false);
        return false;
    }

    public Boolean visit(AbsVarName varName, Object visArg) {
        SemAn.isAddr.put(varName, true);
        return true;
    }

    public Boolean visit(AbsUnExpr unExpr, Object visArg) {
        unExpr.subExpr.accept(this, visArg);
        if(unExpr.oper == AbsUnExpr.Oper.DATA && SemAn.isOfType.get(unExpr.subExpr).actualType() instanceof SemPtrType) {
            SemAn.isAddr.put(unExpr, true);
            return true;
        }
        SemAn.isAddr.put(unExpr, false);
        return false;
    }

    public Boolean visit(AbsArrExpr arrExpr, Object visArg) {
        arrExpr.accept(this, visArg);
        if(arrExpr.array.accept(this, visArg)) {
            SemAn.isAddr.put(arrExpr, true);
            return true;
        }
        SemAn.isAddr.put(arrExpr, false);
        return false;
    }

    public Boolean visit(AbsNewExpr newExpr, Object visArg) {
        SemAn.isAddr.put(newExpr, false);
        return false;
    }

    public Boolean visit(AbsFunName funName, Object visArg) {
        funName.args.accept(this, visArg);
        SemAn.isAddr.put(funName, false);
        return false;
    }

    public Boolean visit(AbsDelExpr delExpr, Object visArg) {
        delExpr.expr.accept(this, visArg);
        SemAn.isAddr.put(delExpr, false);
        return false;
    }

    public Boolean visit(AbsCastExpr castExpr, Object visArg) {
        castExpr.expr.accept(this, visArg);
        SemAn.isAddr.put(castExpr, false);
        return false;
    }




}
