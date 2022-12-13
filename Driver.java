// Import libraries
import org.antlr.v4.runtime.*;
import java.sql.Array;
import java.util.*;
import java.io.*;

// Global class for symbol table elements
class SymbolTableElement {
    String name = null;
    String type = null;
    String value = null;

    // Base constructor
    public SymbolTableElement() {}

    // Symbol table constructors
    public SymbolTableElement(String name, String type) {
        this.name = name;
        this.type = type;
    }
    public SymbolTableElement(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }
    // Symbol table element getters
    public String getName() { return this.name; }
    public String getType() { return this.type; }
    public String getValue() { return this.value; }

    // toString method
    @Override public String toString() {
        if (this.value != null) {
            return ("name " + this.name + " type " + this.type + "value " + this.value + "\n");
        }
        else {
            return ("name " + this.name + " type " + this.type + "\n");
        }
    }
}

// Main Driver class
public class Driver {

    // Main method
    public static void main(String[] args) throws Exception {
        // Reading character stream into program
        CharStream chars = CharStreams.fromStream(System.in);

        // Lexer separates character stream into tokens
        LittleLexer lexer = new LittleLexer(chars);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Parsing tokens to create parse tree
        LittleParser parser = new LittleParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListeners(new VerboseListener());
        ParseTree parseTree = parser.program();

        // Creating walker to walk the parse tree nodes
        ParseTreeWalker treeWalker = new ParseTreeWalker();

        // Building symbol table using the parse tree
        SymbolTableBuilder symbolTableBuilder = new SymbolTableBuilder();
        treeWalker.walk(symbolTableBuilder, parseTree);
        symbolTableBuilder.formattedPrint();
    }

    // Extending BaseErrorListener into a more verbose listener for our program
    public static class VerboseListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLIne,
                                String msg, RecognitionAcceptance e) {
            List<String> stack = ((Parser) recognizer).getRuleInvocatoinStack();
            Collections.reverse(stack);
        }
    }

    // Symbol Table Builder
    public static class SymbolTableBuilder extends LittleBaseListener {
        /*  Creating data structures for the symbol table.
         *  Linked hash map for keeping the order of the symbol table elements
         *  Array list for storing the elements in the symbol table.
         *  Stack for seeing if an element is in scope of the table or not.
         */
        LinkedHashMap<String, ArrayList<SymbolTableElement>> scopedTable = new LinkedHashMap<>();
        ArrayList<SymbolTableElement> symbolTable = new ArrayList<>();
        Stack<String> symbolScope = new Stack<>();

        // Declaring variables for keeping position in symbol table
        int symbolTableIndex = 0;
        int stackIndex = 0;
        int statementBlockCount = 1;
        boolean elseStatement = false;
        boolean errorFound = false;

        // Overriding the standard parser context methods in order to create an AST
        // Enter program context
        @Override public void enterProgram(LitteParser.ProgramContext ctx) {
            symbolScope.push("GLOBAL");
            symbolTable = new ArrayList<>();
            symbolTableIndex = 0;
            scopedTable.put(symbolScope.get(stackIndex), symbolTable);
        }

        // Exit program context
        @Override public void exitProgram(LittleParser.ProgramContext ctx) {
            symbolScope.pop();
        }

        // Variable declaration context
        @Override public void enterVar_decl(LittleParser.Var_declContext ctx) {
            String type = null;
            String[] stringName = new String[0];
            if(ctx.getText().startsWith("INT")) {
                stringName = ctx.getText().substring(3).replace(";", "").split(",");
                type = "INT";
            }
            else if(ctx.getText().startsWith("FLOAT")) {
                stringName = ctx.getText().substring(5).replace(";", "").split(",");
                type = "FLOAT";
            }
            for(int i = 0; i < stringName.length; i++) {
                SymbolTableElement symbolEntry = new SymbolTableElement(stringName[i], type);
                errorFound = checkTable(symbolTable, symbolEntry);
                symbolTable.add(symbolTableIndex, symbolEntry);
                symbolTableIndex++;
            }
            scopedTable.put(symbolScope.get(stackIndex), symbolTable);
        }

        // Parameter Declaration List context
        @Override public void enterParam_decl_list(LittleParser.Param_decl_listContext ctx) {
            String type = null;
            String[] stringName = new String[0];
            if (ctx.getText().startsWith("INT")) {
                stringName = ctx.getText().substring(3).replace(")", "").split(",INT");
                type = "INT";
            } else if (ctx.getText().startswith("FLOAT")) {
                stringName = ctx.getText().substring(5).replace(")", "").split(",FLOAT");
                type = "FLOAT";
            }
            for (int i = 0; i < stringName.length; i++) {
                SymbolTableElement symbolEntry = new SymbolTableElement(stringName[i], type);
                errorFound = checkTable(symbolTable, symbolEntry);
                symbolTable.add(symbolTableIndex, symbolEntry);
                symbolTableIndex++;
            }
            scopedTable.put(symbolScope.get(stackIndex), symbolTable);
        }
        // String declaration context
        @Override public void enterString_decl(LittleParser.String_declContext ctx){
            String name = ctx.id().getText();
            String type = "STRING";
            String value = ctx.str().getText();
            SymbolTableElement symbolEntry = new SymbolTableElement(name, type, value);
            errorFound = checkTable(symbolTable, symbolEntry);
            symbolTable.add(symbolTableIndex, symbolEntry);
            scopedTable.put(symbolScope.get(stackIndex), symbolTable);
            symbolTableIndex++;
        }

        // Enter If control statement context
        @Override public void enterIf_stmt(LittleParser.If_stmtContext ctx) {
            if(ctx.getText().contains("ELSE")) {
                elseStatement = true;
            }
            stackIndex++;
            symbolScope.push("BLOCK " + statementBlockCount);
            statementBlockCount++;
            symbolTable = new ArrayList<>();
            symbolTableIndex = 0;
            scopedTable.put(symbolScope.get(stackIndex), symbolTable);
        }

        // Exit If control statement context
        @Override public void exitIf_stmt(LittleParser.If_stmtContext ctx) {
            if(elseStatement) {
                symbolScope.pop();
                elseStatement = false;
                stackIndex--;
            }
            symbolScope.pop();
            stackIndex--;
        }

        // Enter else control statement context
        @Override public void enterElse_part(LitteParser.Else_partContext ctx) {
            if(elseStatement) {
                stackIndex++;
                symbolScope.push("BLOCK " + statementBlockCount);
                statementBlockCount++;
                symbolTable = new ArrayList<>();
                symbolTableIndex = 0;
                scopedTable.put(symbolScope.get(stackIndex), symbolTable);
            }
        }

        // Exit else control statement context
        @Override public void exitElse_part(LittleParser.Else_partContext ctx){}

        // Enter While control statement context
        @Override public void enterWhile_stmt(LittleParser.While_stmtContext ctx) {
            stackIndex++;
            symbolScope.push("BLOCK " + statementBlockCount);
            statementBlockCount++;
            symbolTable = new ArrayList<>();
            symbolTableIndex = 0;
            scopedTable.put(symbolScope.get(stackIndex), symbolTable);
        }

        // Exit While control statement context
        @Override public void exitWhile_stmt(LittleParser.While_stmtContext ctx) {
            symbolScope.pop();
            stackIndex--;
        }

        // Entering function declaration context
        @Override public void enterFunc_decl(LittleParser.Func_declContext ctx) {
            stackIndex++;
            String functionName = ctx.id().getText();
            symbolScope.push(functionName);
            symbolTable = new ArrayList<>();
            symbolTableIndex = 0;
        }

        // Exiting function declaration context
        @Override public void exitFunc_decl(LittleParser.Func_declContext ctx) {
            symbolScope.pop();
            stackIndex--;
        }

        // Method to check the symbol table for duplicate errors
        public boolean checkTable(ArrayList<SymbolTableElement> table, SymbolTableElement element) {
            for(int i = 0; i < table.size(); i++) {
                if(Objects.equals(table.get(i).getType(), element.getType()) &&
                Objects.equals(table.get(i).getName(), element.getName())) {
                    System.out.println("DECLARATION ERROR " + element.getName());
                    System.exit(0);
                    return true;
                }
            }
            return false;
        }

        // Method to print output formatted
        public void formattedPrint() {
            if(!errorFound) {
                String entry;
                int value;
                for(Map.Entry<String, ArrayList<SymbolTableElement>> element : scopedTable.entrySet()) {
                    entry = element.getKey();
                    if(element.getValue().size() == 0) {
                        System.out.println("Symbol Table " + entry + "\n");
                        continue;
                    }
                    value = element.getValue().size();
                    System.out.println("Symbol table " + entry);
                    for(int i = 0; i < value; i++) {
                        if(Objects.equals(element.getValue().get(i).getType(), "STRING")) {
                            System.out.println("name " + element.getValue().get(i).getName() +
                                    " type " + element.getValue().get(i).getType() +
                                    " value " + element.getValue().get(i). getValue());

                        }
                        else {
                            System.out.print("name " + element.getValue().get(i).getName() +
                                    " type " + element.getValue().get(i).getType());
                        }
                    }
                    System.out.println();
                }
            }
        }
    }
}
