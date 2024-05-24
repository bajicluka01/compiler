/**
 * @author sliva
 */
package compiler.phases.synan;

import compiler.common.report.*;
import compiler.data.symbol.*;
import compiler.data.dertree.*;
import compiler.phases.*;
import compiler.phases.lexan.*;

/**
 * Syntax analysis.
 *
 * @author sliva
 */
public class SynAn extends Phase {

	/** The derivation tree of the program being compiled. */
	public static DerTree derTree = null;

	/** The lexical analyzer used by this syntax analyzer. */
	private final LexAn lexAn;

	/**
	 * Constructs a new phase of syntax analysis.
	 */
	public SynAn() {
		super("synan");
		lexAn = new LexAn();
	}

	@Override
	public void close() {
		lexAn.close();
		super.close();
	}

	/**
	 * The parser.
	 *
	 * This method constructs a derivation tree of the program in the source file.
	 * It calls method {@link #parseSource()} that starts a recursive descent parser
	 * implementation of an LL(1) parsing algorithm.
	 */
	public void parser() {
		currSymb = lexAn.lexer();
		derTree = parseSource();
		if (currSymb.token != Symbol.Term.EOF)
			throw new Report.Error(currSymb, "Unexpected '" + currSymb + "' at the end of a program.");
	}

	/** The lookahead buffer (of length 1). */
	private Symbol currSymb = null;

	/**
	 * Appends the current symbol in the lookahead buffer to a derivation tree node
	 * (typically the node of the derivation tree that is currently being expanded
	 * by the parser) and replaces the current symbol (just added) with the next
	 * input symbol.
	 *
	 * @param node The node of the derivation tree currently being expanded by the
	 *             parser.
	 */
	private void add(DerNode node) {
		if (currSymb == null)
			throw new Report.InternalError();
		node.add(new DerLeaf(currSymb));
		currSymb = lexAn.lexer();
	}

	/**
	 * If the current symbol is the expected terminal, appends the current symbol in
	 * the lookahead buffer to a derivation tree node (typically the node of the
	 * derivation tree that is currently being expanded by the parser) and replaces
	 * the current symbol (just added) with the next input symbol. Otherwise,
	 * produces the error message.
	 *
	 * @param node     The node of the derivation tree currently being expanded by
	 *                 the parser.
	 * @param token    The expected terminal.
	 * @param errorMsg The error message.
	 */
	private void add(DerNode node, Symbol.Term token, String errorMsg) {
		if (currSymb == null)
			throw new Report.InternalError();
		if (currSymb.token == token) {
			node.add(new DerLeaf(currSymb));
			currSymb = lexAn.lexer();
		} else
			throw new Report.Error(currSymb, "Unexpected \'" + currSymb.toString() + "\'");
	}

	private DerNode parseSource() {
		DerNode node = new DerNode(DerNode.Nont.Source);
		node.add(parseDecl());
		node.add(parseDeclsRest());
		//node.add(parseDeclsFirst());
		if(currSymb.token != Symbol.Term.EOF)
			throw new Report.Error(currSymb, "Unexpected \'" + currSymb.toString() + "\'");
		return node;
	}

	private DerNode parseDeclsRest() {
		DerNode node = new DerNode(DerNode.Nont.DeclsRest);
		switch(currSymb.token) {
			case VAR:
				node.add(parseDecl());
				node.add(parseDeclsRest());
				break;
			case TYP:
				node.add(parseDecl());
				node.add(parseDeclsRest());
				break;
			case FUN:
				node.add(parseDecl());
				node.add(parseDeclsRest());
				break;
			case EOF:
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	private DerNode parseDecl() {
		DerNode node = new DerNode(DerNode.Nont.Decl);
		switch(currSymb.token) {
			case VAR:
				add(node, Symbol.Term.VAR, "Unexpected \'" + currSymb.toString() + "\'");
				add(node, Symbol.Term.IDENTIFIER, "Unexpected \'" + currSymb.toString() + "\'");
				add(node, Symbol.Term.COLON, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseType());
				break;
			case TYP:
				add(node, Symbol.Term.TYP, "Unexpected \'" + currSymb.toString() + "\'");
				add(node, Symbol.Term.IDENTIFIER, "Unexpected \'" + currSymb.toString() + "\'");
				add(node, Symbol.Term.COLON, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseType());
				break;
			case FUN:
				add(node, Symbol.Term.FUN, "Unexpected \'" + currSymb.toString() + "\'");
				add(node, Symbol.Term.IDENTIFIER, "Unexpected \'" + currSymb.toString() + "\'");
				add(node, Symbol.Term.LPARENTHESIS, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseFunParams());
				add(node, Symbol.Term.RPARENTHESIS, "Unexpected \'" + currSymb.toString() + "\'");
				add(node, Symbol.Term.COLON, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseType());
				node.add(parseFunStmts());
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");

		}
		return node;
	}

	public DerNode parseFunParams() {
		DerNode node = new DerNode(DerNode.Nont.FunParams);
		switch(currSymb.token) {
			case IDENTIFIER:
				add(node, Symbol.Term.IDENTIFIER, "Unexpected \'" + currSymb.toString() + "\'");
				add(node, Symbol.Term.COLON, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseType());
				node.add(parseFunParamsA());
				break;
			case RPARENTHESIS:
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseFunParamsA() {
		DerNode node = new DerNode(DerNode.Nont.FunParamsRest);
		switch(currSymb.token) {
			case COMMA:
				add(node, Symbol.Term.COMMA, "Unexpected \'" + currSymb.toString() + "\'");
				add(node, Symbol.Term.IDENTIFIER, "Unexpected \'" + currSymb.toString() + "\'");
				add(node, Symbol.Term.COLON, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseType());
				node.add(parseFunParamsA());
				break;
			case RPARENTHESIS:
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseFunStmts() {
		DerNode node = new DerNode(DerNode.Nont.FunStmts);
		switch(currSymb.token) {
			case ASSIGN:
				add(node, Symbol.Term.ASSIGN, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseStmts());
				add(node, Symbol.Term.COLON, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExpr());
				node.add(parseWhereStmt());
				break;
			case TYP:
			case VAR:
			case FUN:
			case RBRACE:
			case EOF:
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseWhereStmt() {
		DerNode node = new DerNode(DerNode.Nont.WhereStmt);
		switch(currSymb.token) {
			case LBRACE:
				add(node, Symbol.Term.LBRACE, "Unexpected \'" + currSymb.toString() + "\'");
				add(node, Symbol.Term.WHERE, "Unexpected \'" + currSymb.toString() + "\'");
				//node.add(parseDecls());
				node.add(parseDecl());
				node.add(parseDeclsA());
				add(node, Symbol.Term.RBRACE, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			case TYP:
			case VAR:
			case FUN:
			case RBRACE:
			case EOF:
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseStmts() {
		DerNode node = new DerNode(DerNode.Nont.Stmts);
		switch(currSymb.token) {
			case IDENTIFIER:
			case LPARENTHESIS:
			case VOIDCONST:
			case BOOLCONST:
			case CHARCONST:
			case INTCONST:
			case IF:
			case WHILE:
			case ADD:
			case SUB:
			case NEW:
			case DEL:
			case DATA:
			case ADDR:
			case PTRCONST:
				node.add(parseStmt());
				node.add(parseStmtsA());
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseStmtsA() {
		DerNode node = new DerNode(DerNode.Nont.StmtsRest);
		switch(currSymb.token) {
			case SEMIC:
				add(node, Symbol.Term.SEMIC, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseStmt());
				node.add(parseStmtsA());
				break;
			case COLON:
			case END:
			case ELSE:
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseDeclsA() {
		DerNode node = new DerNode(DerNode.Nont.DeclsRest);
		switch(currSymb.token) {
			case TYP:
			case VAR:
			case FUN:
				node.add(parseDecl());
				node.add(parseDeclsA());
				break;
			case RBRACE:
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseType() {
		DerNode node = new DerNode(DerNode.Nont.Type);
		switch(currSymb.token) {
			case IDENTIFIER:
				add(node, Symbol.Term.IDENTIFIER, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			case VOID:
				add(node, Symbol.Term.VOID, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			case LPARENTHESIS:
				add(node, Symbol.Term.LPARENTHESIS, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseType());
				add(node, Symbol.Term.RPARENTHESIS, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			case BOOL:
				add(node, Symbol.Term.BOOL, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			case CHAR:
				add(node, Symbol.Term.CHAR, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			case INT:
				add(node, Symbol.Term.INT, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			case ARR:
				add(node, Symbol.Term.ARR, "Unexpected \'" + currSymb.toString() + "\'");
				add(node, Symbol.Term.LBRACKET, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExpr());
				add(node, Symbol.Term.RBRACKET, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseType());
				break;
			case PTR:
				add(node, Symbol.Term.PTR, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseType());
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseStmt() {
		DerNode node = new DerNode(DerNode.Nont.Stmt);
		switch(currSymb.token) {
			case LPARENTHESIS:
			case ADD:
			case SUB:
			case NEW:
			case DEL:
			case DATA:
			case ADDR:
			case PTRCONST:
			case VOIDCONST:
			case BOOLCONST:
			case CHARCONST:
			case INTCONST:
			case IDENTIFIER:
				node.add(parseExpr());
				node.add(parseStmtB());
				break;
			case IF:
				add(node, Symbol.Term.IF, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExpr());
				add(node, Symbol.Term.THEN, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseStmts());
				node.add(parseElseStmt());
				break;
			case WHILE:
				add(node, Symbol.Term.WHILE, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExpr());
				add(node, Symbol.Term.DO, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseStmts());
				add(node, Symbol.Term.END, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseStmtB() {
		DerNode node = new DerNode(DerNode.Nont.AssignStmt);
		switch(currSymb.token) {
			case ASSIGN:
				add(node, Symbol.Term.ASSIGN, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExpr());
				break;
			case COLON:
			case SEMIC:
			case END:
			case ELSE:
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseElseStmt() {
		DerNode node = new DerNode(DerNode.Nont.ElseStmt);
		switch(currSymb.token) {
			case END:
				add(node, Symbol.Term.END, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			case ELSE:
				add(node, Symbol.Term.ELSE, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseStmts());
				add(node, Symbol.Term.END, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseExpr() {
		DerNode node = new DerNode(DerNode.Nont.Expr);
		switch(currSymb.token) {
			case IDENTIFIER:
			case CHARCONST:
			case INTCONST:
			case ADD:
			case SUB:
			case BOOLCONST:
			case VOIDCONST:
			case PTRCONST:
			case DATA:
			case ADDR:
			case NEW:
			case DEL:
			case LPARENTHESIS:
				node.add(parseExprB());
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseExprA() {
		DerNode node = new DerNode(DerNode.Nont.RelExprRest);
		switch(currSymb.token) {
			case TYP:
			case COLON:
			case VAR:
			case FUN:
			case RPARENTHESIS:
			case SEMIC:
			case ASSIGN:
			case LBRACE:
			case RBRACE:
			case RBRACKET:
			case THEN:
			case END:
			case ELSE:
			case DO:
			case COMMA:
			case EOF:
				break;
			case EQU:
				add(node, Symbol.Term.EQU, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExprB());
				break;
			case NEQ:
				add(node, Symbol.Term.NEQ, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExprB());
				break;
			case LTH:
				add(node, Symbol.Term.LTH, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExprB());
				break;
			case GTH:
				add(node, Symbol.Term.GTH,"Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExprB());
				break;
			case LEQ:
				add(node, Symbol.Term.LEQ, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExprB());
				break;
			case GEQ:
				add(node, Symbol.Term.GEQ, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExprB());
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseExprB() {
		DerNode node = new DerNode(DerNode.Nont.RelExpr);
		switch(currSymb.token) {
			case IDENTIFIER:
			case CHARCONST:
			case INTCONST:
			case ADD:
			case SUB:
			case BOOLCONST:
			case VOIDCONST:
			case PTRCONST:
			case DATA:
			case ADDR:
			case NEW:
			case DEL:
			case LPARENTHESIS:
				node.add(parseExprD());
				node.add(parseExprA());
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseExprC() {
		DerNode node = new DerNode(DerNode.Nont.AddExprRest);
		switch(currSymb.token) {
			case TYP:
			case COLON:
			case VAR:
			case FUN:
			case RPARENTHESIS:
			case SEMIC:
			case ASSIGN:
			case LBRACE:
			case RBRACE:
			case RBRACKET:
			case THEN:
			case END:
			case ELSE:
			case DO:
			case COMMA:
			case EOF:
			case EQU:
			case NEQ:
			case GTH:
			case LTH:
			case GEQ:
			case LEQ:
				break;
			case ADD:
				add(node, Symbol.Term.ADD, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExprD());
				node.add(parseExprC());
				break;
			case SUB:
				add(node, Symbol.Term.SUB, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExprD());
				node.add(parseExprC());
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseExprD() {
		DerNode node = new DerNode(DerNode.Nont.AddExpr);
		switch(currSymb.token) {
			case IDENTIFIER:
			case CHARCONST:
			case INTCONST:
			case ADD:
			case SUB:
			case BOOLCONST:
			case VOIDCONST:
			case PTRCONST:
			case DATA:
			case ADDR:
			case NEW:
			case DEL:
			case LPARENTHESIS:
				node.add(parseExprF());
				node.add(parseExprC()); //rest
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseExprE() {
		DerNode node = new DerNode(DerNode.Nont.MulExprRest);
		switch(currSymb.token) {
			case TYP:
			case COLON:
			case VAR:
			case FUN:
			case RPARENTHESIS:
			case SEMIC:
			case ASSIGN:
			case LBRACE:
			case RBRACE:
			case RBRACKET:
			case THEN:
			case END:
			case ELSE:
			case DO:
			case COMMA:
			case EOF:
			case EQU:
			case NEQ:
			case GTH:
			case LTH:
			case GEQ:
			case LEQ:
			case ADD:
			case SUB:
				break;
			case MOD:
				add(node, Symbol.Term.MOD, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExprF());
				node.add(parseExprE());
				break;
			case MUL:
				add(node, Symbol.Term.MUL, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExprF());
				node.add(parseExprE());
				break;
			case DIV:
				add(node, Symbol.Term.DIV, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExprF());
				node.add(parseExprE());
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseExprF() {
		DerNode node = new DerNode(DerNode.Nont.MulExpr);
		switch(currSymb.token) {
			case IDENTIFIER:
			case CHARCONST:
			case INTCONST:
			case BOOLCONST:
			case VOIDCONST:
			case PTRCONST:
			case LPARENTHESIS:
				node.add(parseExprJ());
				break;
			case ADDR:
			case DATA:
				node.add(parseUnop2());
				node.add(parseExprJ());
				break;
			case ADD:
			case SUB:
				node.add(parseUnop1());
				node.add(parseExprJ());
				break;
			case NEW:
				add(node, Symbol.Term.NEW, "Unexpected \'" + currSymb.toString() + "\'");
				add(node, Symbol.Term.LPARENTHESIS, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseType());
				add(node, Symbol.Term.RPARENTHESIS, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			case DEL:
				add(node, Symbol.Term.DEL, "Unexpected \'" + currSymb.toString() + "\'");
				add(node, Symbol.Term.LPARENTHESIS, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExpr());
				add(node, Symbol.Term.RPARENTHESIS, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		node.add(parseExprE());
		return node;
	}

	public DerNode parseExprJ() {
		DerNode node = new DerNode(DerNode.Nont.PrefExpr);
		switch(currSymb.token) {
			case ADD:
				add(node, Symbol.Term.ADD, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExprH());
				break;
			case SUB:
				add(node, Symbol.Term.SUB, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExprH());
				break;
			case DATA:
				add(node, Symbol.Term.DATA, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExprH());
				break;
			case ADDR:
				add(node, Symbol.Term.ADDR, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExprH());
				break;
			case IDENTIFIER:
			case CHARCONST:
			case INTCONST:
			case BOOLCONST:
			case VOIDCONST:
			case PTRCONST:
			case LPARENTHESIS:
				node.add(parseExprH());
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseExprG() {
		DerNode node = new DerNode(DerNode.Nont.PstfExprRest);
		switch(currSymb.token) {
			case TYP:
			case COLON:
			case VAR:
			case FUN:
			case RPARENTHESIS:
			case SEMIC:
			case ASSIGN:
			case LBRACE:
			case RBRACE:
			case RBRACKET:
			case THEN:
			case END:
			case ELSE:
			case DO:
			case COMMA:
			case EOF:
			case EQU:
			case NEQ:
			case GTH:
			case LTH:
			case GEQ:
			case LEQ:
			case ADD:
			case SUB:
			case MUL:
			case DIV:
			case MOD:
				break;
			case LBRACKET:
				add(node, Symbol.Term.LBRACKET, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExpr());
				add(node, Symbol.Term.RBRACKET, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExprG());
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseExprH() {
		DerNode node = new DerNode(DerNode.Nont.PstfExpr);
		switch(currSymb.token) {
			case IDENTIFIER:
			case CHARCONST:
			case INTCONST:
			case BOOLCONST:
			case VOIDCONST:
			case PTRCONST:
				node.add(parseExprI());
				break;
			case LPARENTHESIS:
				add(node, Symbol.Term.LPARENTHESIS, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExpr());
				node.add(parseTypeCast());
				add(node, Symbol.Term.RPARENTHESIS, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		node.add(parseExprG());
		return node;
	}

	public DerNode parseExprI() {
		DerNode node = new DerNode(DerNode.Nont.AtomExpr);
		switch(currSymb.token) {
			case CHARCONST:
				add(node, Symbol.Term.CHARCONST, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			case INTCONST:
				add(node, Symbol.Term.INTCONST, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			case VOIDCONST:
				add(node, Symbol.Term.VOIDCONST, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			case BOOLCONST:
				add(node, Symbol.Term.BOOLCONST, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			case PTRCONST:
				add(node, Symbol.Term.PTRCONST, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			case IDENTIFIER:
				add(node, Symbol.Term.IDENTIFIER, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseCall());
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseCall() {
		DerNode node = new DerNode(DerNode.Nont.CallExpr);
		switch(currSymb.token) {
			case TYP:
			case COLON:
			case VAR:
			case FUN:
			case RPARENTHESIS:
			case SEMIC:
			case ASSIGN:
			case LBRACE:
			case RBRACE:
			case RBRACKET:
			case THEN:
			case END:
			case ELSE:
			case DO:
			case COMMA:
			case EOF:
			case EQU:
			case NEQ:
			case GTH:
			case LTH:
			case GEQ:
			case LEQ:
			case ADD:
			case SUB:
			case MUL:
			case DIV:
			case MOD:
			case LBRACKET:
				break;
			case LPARENTHESIS:
				add(node, Symbol.Term.LPARENTHESIS, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseCallA());
				add(node, Symbol.Term.RPARENTHESIS, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseCallA() {
		DerNode node = new DerNode(DerNode.Nont.CallParams);
		switch(currSymb.token) {
			case IDENTIFIER:
			case CHARCONST:
			case INTCONST:
			case BOOLCONST:
			case VOIDCONST:
			case PTRCONST:
			case LPARENTHESIS:
			case NEW:
			case DEL:
			case ADD:
			case SUB:
			case ADDR:
			case DATA:
				node.add(parseExpr());
				node.add(parseCalls());
				break;
			case RPARENTHESIS:
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseCalls() {
		DerNode node = new DerNode(DerNode.Nont.CallParamsRest);
		switch(currSymb.token) {
			case COMMA:
				add(node, Symbol.Term.COMMA, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseExpr());
				node.add(parseCalls());
				break;
			case RPARENTHESIS:
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseTypeCast() {
		DerNode node = new DerNode(DerNode.Nont.CastExpr);
		switch(currSymb.token) {
			case COLON:
				add(node, Symbol.Term.COLON, "Unexpected \'" + currSymb.toString() + "\'");
				node.add(parseType());
				break;
			case RPARENTHESIS:
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseUnop1() {
		DerNode node = new DerNode(DerNode.Nont.Unop);
		switch(currSymb.token) {
			case ADD:
				add(node, Symbol.Term.ADD, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			case SUB:
				add(node, Symbol.Term.SUB, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}

	public DerNode parseUnop2() {
		DerNode node = new DerNode(DerNode.Nont.Unop);
		switch(currSymb.token) {
			case ADDR:
				add(node, Symbol.Term.ADDR, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			case DATA:
				add(node, Symbol.Term.DATA, "Unexpected \'" + currSymb.toString() + "\'");
				break;
			default:
				throw new Report.Error("Unexpected \'" + currSymb.toString() + "\'");
		}
		return node;
	}
}
