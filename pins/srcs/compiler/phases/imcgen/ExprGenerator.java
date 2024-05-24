package compiler.phases.imcgen;
import java.util.*;
import compiler.common.report.Report;
import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.data.imcode.*;
import compiler.data.layout.*;
import compiler.data.type.*;
import compiler.phases.frames.*;
import compiler.phases.seman.*;
/**
 * @author sliva
 */
public class ExprGenerator implements AbsVisitor<ImcExpr, Stack<Frame>> {

    public ImcExpr visit(AbsFunDef funDef, Stack<Frame> visArg) {
        //ImcExpr e = (ImcExpr)super.visit(funDef, visArg);
        return null;
    }


    public ImcExpr visit(AbsAtomExpr atomExpr, Stack<Frame> visArg) {
        //System.out.println("test exprgen");
        ImcGen.exprImCode.put(atomExpr, new ImcCONST(0));
        return null;
    }


}
