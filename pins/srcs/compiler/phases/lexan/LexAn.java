/**
 * @author sliva
 */
package compiler.phases.lexan;

import java.io.*;
import compiler.common.report.*;
import compiler.data.symbol.*;
import compiler.phases.*;

/**
 * Lexical analysis.
 *
 * @author sliva
 */
public class LexAn extends Phase {

	/** The name of the source file. */
	private final String srcFileName;

	/** The source file reader. */
	private final BufferedReader srcFile;

	/**
	 * Constructs a new phase of lexical analysis.
	 */
	public LexAn() {
		super("lexan");
		srcFileName = compiler.Main.cmdLineArgValue("--src-file-name");
		try {
			srcFile = new BufferedReader(new FileReader(srcFileName));
		} catch (IOException ___) {
			throw new Report.Error("Cannot open source file '" + srcFileName + "'.");
		}
	}

	@Override
	public void close() {
		try {
			srcFile.close();
		} catch (IOException ___) {
			Report.warning("Cannot close source file '" + this.srcFileName + "'.");
		}
		super.close();
	}

	/**
	 * The lexer.
	 *
	 * This method returns the next symbol from the source file. To perform the
	 * lexical analysis of the entire source file, this method must be called until
	 * it returns EOF. This method calls {@link #lexify()}, logs its result if
	 * requested, and returns it.
	 *
	 * @return The next symbol from the source file or EOF if no symbol is available
	 *         any more.
	 */
	public Symbol lexer() {
		Symbol symb = lexify();
		if (symb.token != Symbol.Term.EOF)
			symb.log(logger);
		return symb;
	}

	/**
	 * Performs the lexical analysis of the source file.
	 *
	 * This method returns the next symbol from the source file. To perform the
	 * lexical analysis of the entire source file, this method must be called until
	 * it returns EOF.
	 *
	 * @return The next symbol from the source file or EOF if no symbol is available
	 *         any more.
	 */
	private Symbol lexify() {
		try {
			while(srcFile.ready() || buffer != ' ') {
				char c = (char)srcFile.read();
				boolean found = false;

				if(nl) {
					countWhiteSpace = 0;
					columnCount = 0;
					currentLineWidth = 0;
					nl = false;
				}

				// ignore comments
				if(c == '#' && srcFile.ready()) {
					srcFile.readLine();
					lineCount++;
					currentLineWidth = 0;
					countWhiteSpace = 0;
					columnCount = 0;
					continue;
				}

				if(current.isEmpty() && buffer == ' ') {
					column = columnCount;
					currentLine = lineCount;
				}

				if(c == '\'') {
					charConst = true;
					/*char c1 = (char)srcFile.read();
					char c2 = (char)srcFile.read();
					System.out.println(c1);
					if(c1 >= 32 && c1 <= 126)
						return new Symbol(Symbol.Term.CHARCONST, c + "" + c1 + "" + c2, new Location(currentLine,currentLineWidth - 3 + 1,currentLine,currentLineWidth));
					else
						throw new Report.Error(new Location(currentLine, currentLineWidth - 3 + 1 - 0, currentLine, currentLineWidth - 0), "Invalid char const");
					*/
				}



				// append previous char to current string
				if(buffer != ' ') {
					for(int i = 0; i < symbols.length; i++) {
						if(buffer == symbols[i]) {
							found = true;
							break;
						}
					}
					current = buffer + "";
					buffer = c;
				}

				// check if current char is a symbol
				for (int i = 0; i < symbols.length; i++) {
					if (c == symbols[i]) {
						found = true;
						buffer = c;
						break;
					}
				}

				// change current location
				if(c == 9) {
					columnCount += 4;
					countWhiteSpace += 4;
				}
				else if(c == 10) {
					nl = true;
					lineCount++;
				}
				else
					columnCount++;

				if(c == 32)
					countWhiteSpace++;

				// checks if int const followed by identifier (e.g. 123xy)
				if(isNum(current) && !isNum(c+"")) {
					found = true;
					buffer = c;
				}

				// checks if char constant is valid
				if(charConst) {
					String tmp = current;
					if(current.length() > 3) {
						currentLineWidth += tmp.length() + countWhiteSpace;
						countWhiteSpace = 0;
						throw new Report.Error(new Location(currentLine, currentLineWidth - tmp.length() + 1, currentLine, currentLineWidth), "Invalid char const");
					}
					else if(current.length() == 3) {
						if(current.charAt(0) == '\'' && current.charAt(2) == '\'' &&  current.charAt(1) >= 32 && current.charAt(1) <= 126) {
							current = "";
							charConst = false;
							column = columnCount - 1;
							currentLineWidth += tmp.length() + countWhiteSpace;
							countWhiteSpace = 0;
							return new Symbol(Symbol.Term.CHARCONST, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1,currentLine,currentLineWidth));
						}
					}
				}

				if(c == '\n' || c == '\t' || c == ' ' || c==65535 || c == 13) { //65535 = EOF
					found = true;
					buffer = ' ';
				}
				else if(!found){
					current += c;
					buffer = ' ';
				}

				if(found && !current.equals("")) {
					String tmp = current;
					current = "";
					column = columnCount - 1;
					return returnValue(tmp, c);
				}

			}
			// return last symbol, after eof reached
			if(!current.equals("")) {
				String tmp = current;
				current = "";
				return returnValue(tmp, '0');
			}
			else if(buffer != ' ') {
				return returnValue(buffer+"", '0');
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		return new Symbol(Symbol.Term.EOF, "eof", new Location(currentLine,column,currentLine,column));
	}

	private Symbol returnValue(String tmp, char c) {
		currentLineWidth += tmp.length() + countWhiteSpace;
		countWhiteSpace = 0;
		int d = 0;
		if(c == ' ')
			d++;

		switch(tmp) {
			case "arr":   return new Symbol(Symbol.Term.ARR, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "bool":  return new Symbol(Symbol.Term.BOOL, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "char":  return new Symbol(Symbol.Term.CHAR, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "del":   return new Symbol(Symbol.Term.DEL, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "do":    return new Symbol(Symbol.Term.DO, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "else":  return new Symbol(Symbol.Term.ELSE, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "end":   return new Symbol(Symbol.Term.END, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "fun":   return new Symbol(Symbol.Term.FUN, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "if":    return new Symbol(Symbol.Term.IF, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "int":   return new Symbol(Symbol.Term.INT, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "new":   return new Symbol(Symbol.Term.NEW, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "ptr":   return new Symbol(Symbol.Term.PTR, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "then":  return new Symbol(Symbol.Term.THEN, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "typ":   return new Symbol(Symbol.Term.TYP, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "var":   return new Symbol(Symbol.Term.VAR, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "void":  return new Symbol(Symbol.Term.VOID, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "where": return new Symbol(Symbol.Term.WHERE, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "while": return new Symbol(Symbol.Term.WHILE, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "none":  return new Symbol(Symbol.Term.VOIDCONST, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "true":  return new Symbol(Symbol.Term.BOOLCONST, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "false": return new Symbol(Symbol.Term.BOOLCONST, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "null":  return new Symbol(Symbol.Term.PTRCONST, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "!":
				if(buffer == '=') {
					buffer = ' ';
					return new Symbol(Symbol.Term.NEQ, "!=", new Location(currentLine, currentLineWidth - d, currentLine, currentLineWidth + 1 - d));
				}
				else {
					throw new Report.Error(new Location(currentLine, currentLineWidth - d, currentLine, currentLineWidth + 1 - d), "Unexpected character \"!\"");
				}
			case "<":
				if(buffer == '=') {
					buffer = ' ';
					return new Symbol(Symbol.Term.LEQ, "<=", new Location(currentLine, currentLineWidth - d, currentLine, currentLineWidth + 1 - d));
				}
				else
					return new Symbol(Symbol.Term.LTH, tmp, new Location(currentLine, currentLineWidth - tmp.length() + 1 - d, currentLine, currentLineWidth - d));
			case ">":
				if(buffer == '=') {
					buffer = ' ';
					return new Symbol(Symbol.Term.GEQ, ">=", new Location(currentLine, currentLineWidth - d, currentLine, currentLineWidth + 1 - d));
				}
				else
					return new Symbol(Symbol.Term.GTH, tmp, new Location(currentLine, currentLineWidth - tmp.length() + 1 - d, currentLine, currentLineWidth - d));
			case "+":     return new Symbol(Symbol.Term.ADD, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "-":     return new Symbol(Symbol.Term.SUB, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "*":     return new Symbol(Symbol.Term.MUL, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "/":     return new Symbol(Symbol.Term.DIV, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "%":     return new Symbol(Symbol.Term.MOD, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "$":     return new Symbol(Symbol.Term.DATA, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "@":     return new Symbol(Symbol.Term.ADDR, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "=":
				if(buffer == '=') {
					buffer = ' ';
					return new Symbol(Symbol.Term.EQU, "==", new Location(currentLine, currentLineWidth - d, currentLine, currentLineWidth + 1 - d));
				}
				else
					return new Symbol(Symbol.Term.ASSIGN, tmp, new Location(currentLine, currentLineWidth - tmp.length() + 1 - d, currentLine, currentLineWidth - d));
			case ",":     return new Symbol(Symbol.Term.COMMA, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case ":":     return new Symbol(Symbol.Term.COLON, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case ";":     return new Symbol(Symbol.Term.SEMIC, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "[":     return new Symbol(Symbol.Term.LBRACKET, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "]":     return new Symbol(Symbol.Term.RBRACKET, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "(":     return new Symbol(Symbol.Term.LPARENTHESIS, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case ")":     return new Symbol(Symbol.Term.RPARENTHESIS, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "{":     return new Symbol(Symbol.Term.LBRACE, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
			case "}":     return new Symbol(Symbol.Term.RBRACE, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
		}
		if(isNum(tmp))
			return new Symbol(Symbol.Term.INTCONST, tmp, new Location(currentLine, currentLineWidth - tmp.length() + 1 - d, currentLine, currentLineWidth - d));
		else if(verifyIdent(tmp))
			return new Symbol(Symbol.Term.IDENTIFIER, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1 - d,currentLine,currentLineWidth - d));
		else
		if(charConst) {
			if(tmp.length() > 3) {  //redundant, to be improved
				currentLineWidth += tmp.length() + countWhiteSpace;
				countWhiteSpace = 0;
				throw new Report.Error(new Location(currentLine, currentLineWidth - tmp.length() + 1, currentLine, currentLineWidth), "Invalid char const");
			}
			else if(tmp.length() == 3) {
				if(tmp.charAt(0) == '\'' && tmp.charAt(2) == '\'' &&  tmp.charAt(1) >= 32 && tmp.charAt(1) <= 126) {
					current = "";
					charConst = false;
					column = columnCount - 1;
					currentLineWidth += tmp.length() + countWhiteSpace;
					countWhiteSpace = 0;
					return new Symbol(Symbol.Term.CHARCONST, tmp, new Location(currentLine,currentLineWidth - tmp.length() + 1,currentLine,currentLineWidth));
				}
			}
			//System.out.println(tmp.length());
			throw new Report.Error(new Location(currentLine, currentLineWidth - tmp.length() + 1 - d, currentLine, currentLineWidth - d), "Invalid char const");
		}
		else
			throw new Report.Error(new Location(currentLine, currentLineWidth - tmp.length() + 1 - d, currentLine, currentLineWidth - d), "Invalid identifier");

	}

	//returns true if input string is number
	private boolean isNum(String str) {
		for(int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if((int)c <= 47 || (int)c >= 58)
				return false;
		}
		return true;
	}

	//returns true if identifier is valid
	private boolean verifyIdent(String str) {
		int ch = str.charAt(0);
		if(!((ch>='a' && ch<='z') || (ch>='A' && ch<='Z')))
			return false;
		for(int i = 1; i < str.length(); i++) {
			char c = str.charAt(i);
			if(!((c>='a' && c<='z') || (c>='A' && c<='Z') || (c>='0' && c<='9') || c=='_'))
				return false;
		}
		return true;
	}

	private static int currentLine = 1;
	private static int column = 0;
	private static int lineCount = 1;
	private static int columnCount = 0;
	private static boolean charConst = false;
	private static String current = "";
	private static char buffer = ' ';
	private static char[] symbols = {'=', '!', '<', '>', '+', '-', '*', '/', '%', '$', '@', ':', ';', '[', ']', '{', '}', '(', ')', '#', ','};
	private static int countWhiteSpace = 0;
	private static int currentLineWidth = 0;
	private static boolean nl = false;


}
