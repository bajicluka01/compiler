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
public class StmtGenerator implements AbsVisitor<ImcStmt, Stack<Frame>> {
    public ImcStmt visit(AbsWhileStmt whileStmt, Stack<Frame> visArg) {
        //ImcExpr e = (ImcExpr)super.visit(funDef, visArg);
        return null;
    }
}
