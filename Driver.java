//import ANTLR's runtime libraries
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.CharStream;
import java.util.Collections;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.Stack;

public class Driver {
	public static class VerboseListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            List<String> stack = ((Parser) recognizer).getRuleInvocationStack();
            Collections.reverse(stack);
        }
    }
	
	public static class SymbolTableBuilder extends LittleBaseListener {
		LinkedHashMap<String, ArrayList<SymbolTableEntry>> tableScope = new LinkedHashMap<>();
		ArrayList<SymbolTableEntry> symbolTable = new ArrayList<>();
		Stack<String> scopes = new Stack<>();
		int syTabIndex = 0;
		int stL = 0;
		int blC = 1;
		boolean hasElseStatement = false;
		boolean hasError = false;
		@Override public void enterProgram(LittleParser.ProgramContext ctx) {
			scopes.push("GLOBAL");
			symbolTable = new ArrayList<>();
			syTabIndex = 0;
			tableScope.put(scopes.get(stL), symbolTable);
		}
		@Override public void exitProgram(LittleParser.ProgramContext ctx) {
			scopes.pop();
		}
		@Override public void enterVar_decl(LittleParser.Var_declContext ctx) {
			String type = null;
			String[] strName = new String[0];
			if (ctx.getText().startsWith("INT")) {
				strName = ctx.getText().substring(3).replace(";", "").split(",");
				type = "INT";
			}
			else if (ctx.getText().startsWith("FLOAT")) {
				strName = ctx.getText().substring(5).replace(";", "").split(",");
				type = "FLOAT";
			}
			for (int j = 0; j < strName.length; j++) {
				SymbolTableEntry symbolEntry = new SymbolTableEntry(strName[j], type);
				hasError = checkTable(symbolTable, symbolEntry);
				symbolTable.add(syTabIndex, symbolEntry);
				syTabIndex++;
			}
			tableScope.put(scopes.get(stL), symbolTable);
		}
		@Override public void enterParam_decl_list(LittleParser.Param_decl_listContext ctx) {
			String type = null;
			String[] strName = new String[0];
			if (ctx.getText().startsWith("INT")) {
				strName = ctx.getText().substring(3).replace(")", "").split(",INT");
				type = "INT";
			}
			else if (ctx.getText().startsWith("FLOAT")) {
				strName = ctx.getText().substring(5).replace(")", "").split(",FLOAT");
				type = "FLOAT";
			}
			for (int j = 0; j < strName.length; j++) {
				SymbolTableEntry symbolEntry = new SymbolTableEntry(strName[j], type);
				hasError = checkTable(symbolTable, symbolEntry);
				symbolTable.add(syTabIndex, symbolEntry);
				syTabIndex++;
			}
			tableScope.put(scopes.get(stL), symbolTable);
		}
		@Override public void enterString_decl(LittleParser.String_declContext ctx) {
			String name = ctx.id().getText();
			String type = "STRING";
			String value = ctx.str().getText();
			SymbolTableEntry symbolEntry = new SymbolTableEntry(name, type, value);
			hasError = checkTable(symbolTable, symbolEntry);
			symbolTable.add(syTabIndex, symbolEntry);
			tableScope.put(scopes.get(stL), symbolTable);
			syTabIndex++;
		}
		@Override public void enterIf_stmt(LittleParser.If_stmtContext ctx) {
			if (ctx.getText().contains("ELSE")) {
				hasElseStatement = true;
			}
			stL++;
			scopes.push("BLOCK " + blC);
			blC++;
			symbolTable = new ArrayList<>();
			syTabIndex = 0;
			tableScope.put(scopes.get(stL), symbolTable);
		}
		@Override public void exitIf_stmt(LittleParser.If_stmtContext ctx) {
			if (hasElseStatement) {
				scopes.pop();
				hasElseStatement = false;
				stL--;
			}
			scopes.pop();
			stL--;
		}
		@Override public void enterElse_part(LittleParser.Else_partContext ctx) {
			if (hasElseStatement) {
				stL++;
				scopes.push("BLOCK " + blC);
				blC++;
				symbolTable = new ArrayList<>();
				syTabIndex = 0;
				tableScope.put(scopes.get(stL), symbolTable);
			}
		}
		@Override public void exitElse_part(LittleParser.Else_partContext ctx) { 
		}
		@Override public void enterWhile_stmt(LittleParser.While_stmtContext ctx) {
			stL++;
			scopes.push("BLOCK " + blC);
			blC++;
			symbolTable = new ArrayList<>();
			syTabIndex = 0;
			tableScope.put(scopes.get(stL), symbolTable);
		}
		@Override public void exitWhile_stmt(LittleParser.While_stmtContext ctx) {
			scopes.pop();
			stL--;
		}
		@Override public void enterFunc_decl(LittleParser.Func_declContext ctx) {
			stL++;
			String func = ctx.id().getText();
			scopes.push(func);
			symbolTable = new ArrayList<>();
			syTabIndex = 0;
		}
		@Override public void exitFunc_decl(LittleParser.Func_declContext ctx) {
			scopes.pop();
			stL--;
		}
		public boolean checkTable(ArrayList<SymbolTableEntry> table, SymbolTableEntry entry) {
			for (int z = 0; z < table.size(); z++) {
				if (Objects.equals(table.get(z).getType(), entry.getType()) && Objects.equals(table.get(z).getName(), entry.getName())) {
					System.out.println("DECLARATION ERROR " + entry.getName());
					System.exit(0);
					return true;
				}
			}
			return false;
		}
		public void prettyPrint() {
			if (!hasError) {
				String k;
				int vc;
				for (Map.Entry<String, ArrayList<SymbolTableEntry>> entry : tableScope.entrySet()) {
					k = entry.getKey();
					if (entry.getValue().size() == 0) {
						System.out.println("Symbol table " + k + "\n");
						continue;
					}
					vc = entry.getValue().size();
					System.out.println("Symbol table " + k);
					for (int p = 0; p < vc; p++) {
						if (Objects.equals(entry.getValue().get(p).getType(), "STRING")) {
							System.out.println("name " + entry.getValue().get(p).getName() + " type " + entry.getValue().get(p).getType() + " value " + entry.getValue().get(p).getValue());
						}
						else {
							System.out.println("name " + entry.getValue().get(p).getName() + " type " + entry.getValue().get(p).getType());
						}
					}
					System.out.println();
				}
			}
		}
	}
    public static void main(String[] args) throws Exception {
        CharStream chars = CharStreams.fromStream(System.in);
        LittleLexer lexer = new LittleLexer(chars);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LittleParser parser = new LittleParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new VerboseListener());
        ParseTree tree = parser.program();
        ParseTreeWalker walker = new ParseTreeWalker();
        SymbolTableBuilder stb = new SymbolTableBuilder();
        walker.walk(stb, tree);
        stb.prettyPrint();
	}
}
class SymbolTableEntry {
	String name = null;
	String type = null;
	String value = null;
	public SymbolTableEntry(){
	}
	public SymbolTableEntry(String name, String type, String value){
		this.name = name;
		this.type = type;
		this.value = value;
	}
	public String getName() {
		return this.name;
	}
	public String getType() {
		return this.type;
	}
	public String getValue() {
		return this.value;
	}
	public SymbolTableEntry(String name, String type){
		this.name = name;
		this.type = type;
	}
	@Override public String toString() {
		if (this.value != null) {
			return ("name " + this.name + " type " + this.type + " value " + this.value + "\n");
		}
		else {
			return ("name " + this.name + " type " + this.type + "\n");
		}
	}
}

