// Import libraries
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.CharStream;
import java.util.Collections;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.List;
import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.Stack;

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

// Global class for AST
class AST {
    AST left;
    AST right;
    String value;
}

// Global class for CodeObject
class CodeObject {
    String code;
    String temp;
    String type;

    public CodeObject(String code, String temp, String type) {
        this.code = code;
        this.temp = temp;
        this.type = type;
    }

    public String getCode() {
        return this.code;
    }

    public String getTemp() {
        return this.temp;
    }

    public String getType() {
        return this.type;
    }

    @Override public String toString() {
        if(this.code.equals("")) {
            return this.temp;
        }
        return this.code;
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
        parser.addErrorListener(new VerboseListener());
        ParseTree parseTree = parser.program();

        // Creating walker to walk the parse tree nodes
        ParseTreeWalker treeWalker = new ParseTreeWalker();

        // Building symbol table using the parse tree
        SymbolTableBuilder symbolTableBuilder = new SymbolTableBuilder();
        treeWalker.walk(symbolTableBuilder, parseTree);
        symbolTableBuilder.formattedPrint();

        // Walking the parse tree again to create the AST
        ASTBuilder ast = new ASTBuilder();
        treeWalker.walk(ast, parseTree);

        // Generate IR code after walking
        ast.IRCodeGenerator();

        // Generate tiny code from IR code
        ast.tinyCodeFormatting();

    }

    // Extending BaseErrorListener into a more verbose listener for our program
    public static class VerboseListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLIne, String msg, RecognitionException e) {
            List<String> stack = ((Parser) recognizer).getRuleInvocationStack();
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
        @Override public void enterProgram(LittleParser.ProgramContext ctx) {
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
            } else if (ctx.getText().startsWith("FLOAT")) {
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
        @Override public void enterElse_part(LittleParser.Else_partContext ctx) {
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
                                    " value " + element.getValue().get(i). getValue() + "\n");

                        }
                        else {
                            System.out.print("name " + element.getValue().get(i).getName() +
                                    " type " + element.getValue().get(i).getType() + "\n");
                        }
                    }
                    System.out.println();
                }
            }
        }
    }

    // Builder for the AST
    public static class ASTBuilder extends LittleBaseListener{
        Hashtable<String, String> variableType;
        Hashtable<Integer, String> variableOrder;
        Hashtable<String, String> stringValue;
        Stack<AST> astTrees;
        Stack<CodeObject> irRepCode;
        Stack<String> tinyCode;
        int tempIRNumber = 0;
        int  variableCount = 0;

        // Constructor
        public ASTBuilder() {
            variableType = new Hashtable<>();
            variableOrder = new Hashtable<>();
            stringValue = new Hashtable<>();
            astTrees = new Stack<>();
            irRepCode = new Stack<CodeObject>();
            tinyCode = new Stack<>();
        }

        // Float or Integer variables AST nodes
        @Override public void enterVar_decl(LittleParser.Var_declContext ctx) {
            String[] variables = (ctx.id_list().getText()).split(",");

            for(int i = 0; i < variables.length; i++) {
                variableType.put(variables[i], ctx.var_type().getText());
                variableOrder.put(variableCount, variables[i]);
                variableCount++;
            }
        }

        // Assignment AST nodes
        @Override public void enterAssign_expr(LittleParser.Assign_exprContext ctx) {
            AST root = new AST();
            root.value = ":=";
            AST leftNode = new AST();
            leftNode.value = "VARREF " + ctx.id().getText() + " " + variableType.get(ctx.id().getText());
            root.left = leftNode;
            astTrees.push(root);
        }

        // AST nodes for addition or subtraction
        @Override public void enterAddop(LittleParser.AddopContext ctx) {
            AST root = astTrees.pop();
            AST rightNode = new AST();
            if(root.right != null) {
                AST temp = root.right;
                rightNode.value = "ADDOP " + ctx.getText();
                rightNode.left = temp;
                root.right = rightNode;
                astTrees.push(root);
                return;
            }
            rightNode.value = "ADDOP " + ctx.getText();
            root.right = rightNode;
            astTrees.push(root);
        }

        // Multiplication or division node for AST
	    @Override public void enterMulop(LittleParser.MulopContext ctx) {
            AST root = astTrees.pop();
            AST rightNode = new AST();
            if(root.right != null){
                AST temp = root.right;
                rightNode.value = "MULOP " + ctx.getText();
                rightNode.left = temp; //ex: e+f 'e' will be the leftchild of '+'
                root.right = rightNode;
                astTrees.push(root);
                return;
            }
            rightNode.value = "MULOP " + ctx.getText();
            root.right = rightNode;
            astTrees.push(root);
	    }

        // AST node for String declaration.
        @Override public void enterString_decl(LittleParser.String_declContext ctx) {
		    AST root = new AST();
            root.value = "STRING " + ctx.id().getText() + " " + ctx.str().getText();
            variableType.put(ctx.id().getText(), "STRING");
            variableOrder.put(variableCount, ctx.id().getText());
            stringValue.put(ctx.id().getText(), ctx.str().getText());
            variableCount++;
            astTrees.push(root);
	    }

        // AST node for primary
        @Override public void enterPrimary(LittleParser.PrimaryContext ctx) {
            AST root = astTrees.pop();
            AST rightNode = root.right;
            String mainValue = ctx.getText();
    
            boolean number = true;
            boolean parenthesisExpr = mainValue.startsWith("(");
            number = (mainValue).matches("-?\\d+(\\.\\d+)?");
            if( number ) {
                mainValue = "CONSTANT " + ctx.getText();
            } else {
                mainValue = "VARREF " + ctx.getText() + " " + variableType.get(ctx.getText());
            }
    
            if(rightNode == null){
                rightNode = new AST();
                rightNode.value = mainValue;
    
                // Ignoring parantheses 
                if(parenthesisExpr){
                    root.right = null;
                } 
                else {
                    root.right = rightNode;
                }
            } else if(rightNode.left == null) {
                AST temp = new AST();
                temp.value = mainValue;
                rightNode.left = temp;
            } 
            else if(rightNode.right == null) {
                AST temp = new AST();
                temp.value = mainValue;
                rightNode.right = temp;
            }
            astTrees.push(root);
        }

        // AST node for read statement
	    @Override public void enterRead_stmt(LittleParser.Read_stmtContext ctx) {
		AST root = new AST();
		root.value = "READ " + (ctx.id_list().getText());
		astTrees.push(root);
	    }

        // AST node for write statement
	    @Override public void enterWrite_stmt(LittleParser.Write_stmtContext ctx) {
		AST root = new AST();
		root.value = "WRITE " + (ctx.id_list().getText());
		astTrees.push(root);
	    }

        // Getter for ASTs
        public Stack<AST> getASTs() {
            return astTrees;
        }

        // Printing AST in post-order traversal method
        public void printAST() {
            for(AST tree: astTrees) {
                ArrayList<String> output = postOrderTraversal(tree);
                System.out.println(output);
            }
        }

        // Method to traverse AST in postorder
        ArrayList<String> postOrderTraversal(AST node) {
            Stack<AST> stack1 = new Stack<AST>();
            ArrayList<String> list = new ArrayList<String>();

            if(node == null) {
                return list;
            }
            stack1.push(node);
            AST prev = null;
            while(!stack1.isEmpty()) {
                AST current = stack1.peek();
                if(prev == null || prev.left == current || prev.right == current) {
                    if(current.left != null) {
                        stack1.push(current.left);
                    }
                    else if(current.right != null) {
                        stack1.push(current.right);
                    }
                    else {
                        stack1.pop();
                        list.add(current.value);
                    }
                }
                else if(current.left == prev) {
                    if(current.right != null) {
                        stack1.pop();
                        list.add(current.value);
                    }
                    prev = current;
                }
	    } 
	    return list;
        }

        // IR code generator
        public void IRCodeGenerator() {
            AST prev = null;
		irRepCode.push(new CodeObject(";IR code\n;LABEL main\n;LINK", "", ""));
            for(AST tree: astTrees) {
                Stack<AST> stack2 = new Stack<AST>();
                if(tree == null) {
                    return;
                }
                stack2.push(tree);
                while(!stack2.isEmpty()){
                    AST current = stack2.peek();
                    if(prev == null || prev.left == current || prev.right == current) {
                        if(current.left != null) {
                            stack2.push(current.left);
                        }
                        else if(current.right != null) {
                            stack2.push(current.right);
                        }
                        else {
                            stack2.pop();
                            IRCodeConvert(current.value);
                        }
                    }
                    else if(current.left == prev) {
                        if(current.right != null) {
                            stack2.push(current.right);
                        }
                        else {
                            stack2.pop();
                            IRCodeConvert(current.value);
                        }
                    }
                    else if(current.right == prev) {
                        stack2.pop();
                        IRCodeConvert(current.value);
                    }
                    prev = current;
                }
            }
            irRepCode.push(new CodeObject("\n;RET\n;tiny code\n", "", ""));
            for(CodeObject object: irRepCode) {
                System.out.print(object.toString());
            }
        }

        // Converting IR code to Assembly
        public void IRCodeConvert(String stringInput) {
            String[] array1 = stringInput.split(" ");
            String code = "";
            String temp = "";
            String type = "";
            String[] allVariables;
            CodeObject rightSide;
            CodeObject leftSide;

            switch(array1[0]) {
                case "VARREF":
                    CodeObject variable = new CodeObject("", array1[1], array1[2]);
                    irRepCode.push(variable);
                    break;
                    
                case "ADDOP":
                    rightSide = irRepCode.pop();
                    leftSide = irRepCode.pop();
                    temp = generateTemp();
                    type = rightSide.getType();
                    code = leftSide.getCode();
                    code += rightSide.getCode();
    
                    if(leftSide.getType().equals("CONSTANT")){
                        code += "\n;STORE" + type.charAt(0) + " " +  leftSide.getTemp() + " " + temp;
                        leftSide.temp = temp;
                        temp = generateTemp();
                    }
                    if(rightSide.getType().equals("CONSTANT")){
                        code += "\n;STORE" + type.charAt(0) + " " +  rightSide.getTemp() + " " + temp;
                        rightSide.temp = temp;
                        temp = generateTemp();
                    }
    
                    if(array1[1].equals("+")){
                        code += "\n;" + "ADD"+ type.charAt(0) + " " + leftSide.getTemp() + " " + rightSide.getTemp() + " " + temp;
                    } else if( array1[1].equals("-")){
                        code += "\n;" + "SUB"+ type.charAt(0) + " " + leftSide.getTemp() + " " + rightSide.getTemp() + " " + temp;
                    }
                    CodeObject addExpr = new CodeObject(code, temp, type);
                    irRepCode.push(addExpr);
                    break;

                case "MULOP":
                    rightSide = irRepCode.pop();
                    leftSide = irRepCode.pop();
                    temp = generateTemp();
                    type = leftSide.getType();
                    code = leftSide.getCode();
                    code += rightSide.getCode();
    
                    if(leftSide.getType().equals("CONSTANT")){
                        code += "\n;STORE" + type.charAt(0) + " " +  leftSide.getTemp() + " " + temp;
                        leftSide.temp = temp;
                        temp = generateTemp();
                    }
                    if(rightSide.getType().equals("CONSTANT")){
                        code += "\n;STORE" + type.charAt(0) + " " +  rightSide.getTemp() + " " + temp;
                        rightSide.temp = temp;
                        temp = generateTemp();
                    }
    
                    if(array1[1].equals("*")){
                        code += "\n;" + "MULT"+ type.charAt(0) + " " + leftSide.getTemp() + " " + rightSide.getTemp() + " " + temp;
                    } 
                    else if(array1[1].equals("/")){
                        code += "\n;" + "DIV"+ type.charAt(0) + " " + leftSide.getTemp() + " " + rightSide.getTemp() + " " + temp;
                    }
                    CodeObject mulExpr = new CodeObject(code, temp, type);
                    irRepCode.push(mulExpr);
                    break;

                case "CONSTANT":
				    CodeObject constant = new CodeObject("", array1[1], "CONSTANT");
				    irRepCode.push(constant);
				    break;

                case ":=":
                    rightSide = irRepCode.pop();
                    leftSide = irRepCode.pop();
                    if(rightSide.getCode().equals("")) {
                        if(rightSide.getType().equals("CONSTANT")) {
                            temp = generateTemp();
                            code = "\n;STORE" + leftSide.getType().charAt(0) + " " +  rightSide.getTemp() + " " + temp;
                            code += "\n;STORE" + leftSide.getType().charAt(0) + " " + temp + " " + leftSide.getTemp();
                            CodeObject simpleAssign = new CodeObject(code, "", "");
                            irRepCode.push(simpleAssign);
                            break;
                        } 
                        else if(rightSide.getType().equals("FLOAT")) {
                            temp = generateTemp();
                            code = rightSide.getCode();
                            code += "\n;STOREF " +  rightSide.getTemp() + " " + temp;
                            code += "\n;STOREF " + temp + " " + leftSide.getTemp();
                            CodeObject simpleAssign = new CodeObject(code, "", "");
                            irRepCode.push(simpleAssign);
                            break;
                        } 
                        else if(rightSide.getType().equals("INT")) {
                            code = rightSide.getCode();
                            code += "\n;STOREI " + rightSide.getTemp() + " " + leftSide.getTemp();
                            CodeObject simpleAssign = new CodeObject(code, "", "");
                            irRepCode.push(simpleAssign);
                            break;
                        }
                    }
                    code = rightSide.getCode();
                    code += "\n;STORE" + rightSide.getType().charAt(0) + " " + rightSide.getTemp() + " " + leftSide.getTemp();
                    CodeObject simpleAssignEquation = new CodeObject(code, "", "");
                    irRepCode.push(simpleAssignEquation);
                    break;

                case "READ":
                    allVariables = array1[1].split(",");
                    code = "";
                    for(String readVar: allVariables){
                        code += "\n;READ" + variableType.get(readVar).charAt(0) + " " + readVar;
                    }
                    CodeObject readObj = new CodeObject(code, "", "");
                    irRepCode.push(readObj);
                    break;

                case "WRITE":
                    allVariables = array1[1].split(",");
                    code = "";
                    for(String writeVar: allVariables) {
                        code += "\n;WRITE" + variableType.get(writeVar).charAt(0) + " " + writeVar;
                    }
                    CodeObject writeObj = new CodeObject(code, "", "");
                    irRepCode.push(writeObj);
                    break;
            }
        }
        
        // Formatting tiny code
        public void tinyCodeFormatting() {
            tempIRNumber = -1;
            for(int i = 0; i < variableCount; i++){
                String tinyType = variableType.get(variableOrder.get(i));
                if (tinyType.equals("INT") || tinyType.equals("FLOAT")){
                    tinyCode.push("var "+ variableOrder.get(i));
                } 
                else if(tinyType.equals("STRING")) {
                    String tinyStringValue = variableType.get(variableOrder.get(i));
                    tinyCode.push("str " + variableOrder.get(i) + " " + variableType.get(variableOrder.get(i)));
                }
            }
            
            for(CodeObject object: irRepCode) {
                String[] temp = object.toString().split("\n");
                for(String string2: temp){
                    ConvertIRToTinyCode(string2);
                }
            }

            tinyCode.push("sys halt");

            for(String string3: tinyCode) {
                System.out.println(string3);
            }
        }

        // Converting IR to Tiny code
        public void ConvertIRToTinyCode(String string4) {
            String[] array2 = string4.split(" ");
            String operation1;
            String operation2;
            String register;
		    if(array2[0].startsWith(";STORE")) {
			    if(array2[1].startsWith("$T")) {
				    operation1 = "r"+array2[1].replace("$T", "");
			    } 
                else {
				operation1 = array2[1];
			}

			if(array2[2].startsWith("$T")){
				operation2 = "r"+ array2[2].replace("$T", "");
			} 
            else {
				operation2 = array2[2];
			}
			tinyCode.push("move " + operation1 + " " + operation2);
		}
		else if(array2[0].equals(";READI")){
			tinyCode.push( "sys readi " + array2[1]);
		}
		else if(array2[0].equals(";READF")) {
			tinyCode.push( "sys readr " + array2[1]);
		}
		else if(array2[0].equals(";WRITEI")) {
			tinyCode.push( "sys writei " + array2[1]);
		}
		else if(array2[0].equals(";WRITEF")) {
			tinyCode.push( "sys writer " + array2[1]);
		}
		else if(array2[0].equals(";WRITES")) {
			tinyCode.push( "sys writes " + array2[1]);
		}
		else if(array2[0].equals(";MULTI")){
			register = " r" + array2[3].replace("$T", "");
			if(array2[1].startsWith("$T")){
				array2[1] = "r" + array2[1].replace("$T", "");
			}
			if(array2[2].startsWith("$T")){
				array2[2] = "r"+ array2[2].replace("$T", "");
			}
			tinyCode.push("move " + array2[1] + register );
			tinyCode.push("muli " + array2[2] + register);
		}
		else if(array2[0].equals(";MULTF")){
			register = " r" + array2[3].replace("$T", "");
			if(array2[1].startsWith("$T")){
				array2[1] = "r" + array2[1].replace("$T", "");
			}
			if(array2[2].startsWith("$T")){
				array2[2] = "r" + array2[2].replace("$T", "");
			}
			tinyCode.push("move " + array2[1] + register );
			tinyCode.push("mulr " + array2[2] + register);
		}
		else if(array2[0].equals(";DIVI")){
			register = " r" + array2[3].replace("$T", "");
			if(array2[1].startsWith("$T")){
				array2[1] = "r" + array2[1].replace("$T", "");
			}
			if(array2[2].startsWith("$T")){
				array2[2] = "r" + array2[2].replace("$T", "");
			}
			tinyCode.push("move " + array2[1] + register );
			tinyCode.push("divi " + array2[2] + register);
		}
		else if(array2[0].equals(";DIVF")){
			register = " r" + array2[3].replace("$T", "");
			if(array2[1].startsWith("$T")){
				array2[1] = "r" + array2[1].replace("$T", "");
			}
			if(array2[2].startsWith("$T")){
				array2[2] = "r" + array2[2].replace("$T", "");
			}

			tinyCode.push("move " + array2[1] + register );
			tinyCode.push("divr " + array2[2] + register);
		}
		else if(array2[0].equals(";ADDI")){
			register = " r" + array2[3].replace("$T", "");
			if(array2[1].startsWith("$T")){
				array2[1] = "r" + array2[1].replace("$T", "");
			}
			if(array2[2].startsWith("$T")){
				array2[2] = "r" + array2[2].replace("$T", "");
			}
			tinyCode.push("move " + array2[1] + register );
			tinyCode.push("addi " + array2[2] + register);
		}
		else if(array2[0].equals(";ADDF")){
			register = " r" + array2[3].replace("$T", "");
			if(array2[1].startsWith("$T")){
				array2[1] = "r" + array2[1].replace("$T", "");
			}
			if(array2[2].startsWith("$T")){
				array2[2] = "r" + array2[2].replace("$T", "");
			}
			tinyCode.push("move " + array2[1] + register );
			tinyCode.push("addr " + array2[2] + register);
		}
		else if(array2[0].equals(";SUBI")){
			register = " r" + array2[3].replace("$T", "");
			if(array2[1].startsWith("$T")){
				array2[1] = "r" + array2[1].replace("$T", "");
			}
			if(array2[2].startsWith("$T")){
				array2[2] = "r" + array2[2].replace("$T", "");
			}
			tinyCode.push("move " + array2[1] + register );
			tinyCode.push("subi " + array2[2] + register);
		}
		else if(array2[0].equals(";SUBF")){
			register = " r" + array2[3].replace("$T", "");
			if(array2[1].startsWith("$T")){
				array2[1] = "r" + array2[1].replace("$T", "");
			}
			if(array2[2].startsWith("$T")){
				array2[2] = "r" + array2[2].replace("$T", "");
			}
			tinyCode.push("move " + array2[1] + register );
			tinyCode.push("subr " + array2[2] + register);
		    }
	    }
        
        /*Generates temp for IR Code representation*/
	    public String generateTemp(){
		tempIRNumber++;
		return "$T" + tempIRNumber;
    }
}

}

