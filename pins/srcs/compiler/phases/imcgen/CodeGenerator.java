package compiler.phases.imcgen;
import java.util.*;

import compiler.common.report.Location;
import compiler.common.report.Report;
import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.data.imcode.*;
import compiler.data.layout.*;
import compiler.data.type.*;
import compiler.phases.frames.*;
import compiler.phases.seman.*;
/**
 * Intermediate code generator.
 *
 * @author sliva
 */
public class CodeGenerator extends AbsFullVisitor<Object, Stack<Frame>> {

    public Object visit(AbsFunDef funDef, Stack<Frame> visArg) {
        ImcExpr e = (ImcExpr)super.visit(funDef, visArg);
        //ImcExpr e = new ExprGenerator().visit(funDef, visArg);
        return null;
    }

    public Object visit(AbsAtomExpr atomExpr, Stack<Frame> visArg) {
        ImcCONST c = null;
        switch(atomExpr.type) {
            case INT:
                c = new ImcCONST(Long.parseLong(atomExpr.expr));
                break;
            case BOOL:
                if(atomExpr.expr.equals("true"))
                    c = new ImcCONST(1);
                else
                    c = new ImcCONST(0);
                break;
            case CHAR:
                c = new ImcCONST((long)atomExpr.expr.charAt(1));
                break;
            case PTR:
            case VOID:
                c = new ImcCONST(0);
        }
        ImcGen.exprImCode.put(atomExpr, c);
        return c;
    }

    public Object visit(AbsVarName varName, Stack<Frame> visArg) {
        ImcNAME name = new ImcNAME(new Label(varName.name));
        ImcGen.exprImCode.put(varName, name);
        return name;
    }

    public Object visit(AbsBinExpr binExpr, Stack<Frame> visArg) {
        ImcBINOP.Oper oper = ImcBINOP.Oper.ADD;
        switch(binExpr.oper) {
            case DIV: oper = ImcBINOP.Oper.DIV;
                break;
            case MOD: oper = ImcBINOP.Oper.MOD;
                break;
            case MUL: oper = ImcBINOP.Oper.MUL;
                break;
            case SUB: oper = ImcBINOP.Oper.SUB;
                break;
            case EQU: oper = ImcBINOP.Oper.EQU;
                break;
            case NEQ: oper = ImcBINOP.Oper.NEQ;
                break;
            case LTH: oper = ImcBINOP.Oper.LTH;
                break;
            case GTH: oper = ImcBINOP.Oper.GTH;
                break;
            case LEQ: oper = ImcBINOP.Oper.LEQ;
                break;
            case GEQ: oper = ImcBINOP.Oper.GEQ;
                break;
        }



        ImcExpr fstExpr = (ImcExpr)binExpr.fstExpr.accept(this, visArg);
        ImcBINOP binop = new ImcBINOP(oper, fstExpr, (ImcExpr)binExpr.sndExpr.accept(this, visArg));
        ImcGen.exprImCode.put(binExpr, binop);
        return binop;
    }

    public Object visit(AbsExprStmt exprStmt, Stack<Frame> visArg) {
        ImcExpr expr = (ImcExpr)exprStmt.expr.accept(this, visArg);
        ImcESTMT estmt = new ImcESTMT(expr);
        ImcGen.stmtImCode.put(exprStmt, estmt);
        return estmt;
    }

    public Object visit(AbsWhileStmt whileStmt, Stack<Frame> visArg) {
        ImcExpr cond = (ImcExpr)whileStmt.cond.accept(this, visArg);
        ImcStmt stmts = (ImcStmt)whileStmt.stmts.accept(this, visArg);

        if(cond == null || stmts == null) //needs fix - while not working
            return null;

        Vector<ImcStmt> imcStmts = new Vector<>();
        Label first = new Label();
        Label second = new Label();
        Label third = new Label();
        imcStmts.add(new ImcLABEL(first));
        imcStmts.add(new ImcCJUMP(cond, second, third));
        imcStmts.add(new ImcLABEL(second));
        imcStmts.add(stmts);
        imcStmts.add(new ImcJUMP(first));
        imcStmts.add(new ImcLABEL(third));

        ImcSTMTS res = new ImcSTMTS(imcStmts);
        ImcGen.stmtImCode.put(whileStmt, res);
        return res;
    }

    public Object visit(AbsIfStmt ifStmt, Stack<Frame> visArg) {
        ImcExpr cond = (ImcExpr)ifStmt.cond.accept(this, visArg);
        ImcStmt thenStmts = (ImcStmt)ifStmt.thenStmts.accept(this, visArg);
        ImcStmt elseStmts = (ImcStmt)ifStmt.elseStmts.accept(this, visArg);
        Vector<ImcStmt> imcStmts = new Vector<>();

        if(cond == null || thenStmts == null)                 //needs fix - if not working
            return null;

        if(elseStmts == null) {
            Label first = new Label();
            Label second = new Label();
            imcStmts.add(new ImcCJUMP(cond, first, second));
            imcStmts.add(new ImcLABEL(first));
            imcStmts.add(thenStmts);
            imcStmts.add(new ImcLABEL(second));
        }
        else {
            Label first = new Label();
            Label second = new Label();
            Label third = new Label();
            imcStmts.add(new ImcCJUMP(cond, first, second));
            imcStmts.add(new ImcLABEL(first));
            imcStmts.add(thenStmts);
            imcStmts.add(new ImcJUMP(third));
            imcStmts.add(new ImcLABEL(second));
            imcStmts.add(elseStmts);
            imcStmts.add(new ImcLABEL(third));
        }

        ImcSTMTS stmts = new ImcSTMTS(imcStmts);
        ImcGen.stmtImCode.put(ifStmt, stmts);
        return stmts;
    }

    public Object visit(AbsFunName funName, Stack<Frame> visArg) {
        Vector<ImcExpr> args = new Vector<>();

        //args.add(new ImcTEMP(new Temp()));
        args.add(new ImcCONST(0)); //najbolj zunanji nivo ne rabi SL

        for(AbsExpr arg : funName.args.args()) {
            ImcExpr expr = (ImcExpr)arg.accept(this, visArg);
            ImcGen.exprImCode.put(arg, expr);
            args.add(expr);
        }



        ImcCALL call = new ImcCALL(Frames.frames.get((AbsFunDecl) SemAn.declaredAt.get(funName)).label, args);

        ImcGen.exprImCode.put(funName, call);
        return call;
    }

    public Object visit(AbsAssignStmt assignStmt, Stack<Frame> visArg) {
        ImcExpr dst = (ImcExpr)assignStmt.dst.accept(this, visArg);
        ImcExpr src = (ImcExpr)assignStmt.src.accept(this, visArg);

        ImcMOVE move = new ImcMOVE(new ImcMEM(dst), src);
        ImcGen.stmtImCode.put(assignStmt, move);
        return move;
    }

    public Object visit(AbsArrExpr arrExpr, Stack<Frame> visArg) {

        return null;
    }

    public Object visit(AbsBlockExpr blockExpr, Stack<Frame> visArg) {
        ImcStmt decls = (ImcStmt)blockExpr.decls.accept(this, visArg);
        ImcStmt stmts = (ImcStmt)blockExpr.stmts.accept(this, visArg);
        ImcExpr expr = (ImcExpr)blockExpr.expr.accept(this, visArg);

        //if(decls == null || stmts == null)
        //    return null;

        ImcSEXPR sexpr = new ImcSEXPR(stmts, expr);
        ImcGen.exprImCode.put(blockExpr, sexpr);
        return sexpr;
    }

    public Object visit(AbsStmts stmts, Stack<Frame> visArg) {
        Vector<ImcStmt> s = new Vector<>();
        for(AbsStmt stmt : stmts.stmts()) {
            s.add((ImcStmt)stmt.accept(this, visArg));
        }
        ImcSTMTS stm = new ImcSTMTS(s);
        //ImcGen.stmtImCode.put()
        return stm;
    }
}