//import ANTLR's runtime libraries
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.CharStream;
import java.util.Collections;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.io.*;

public class Driver {
	public static class VerboseListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            List<String> stack = ((Parser) recognizer).getRuleInvocationStack();
            Collections.reverse(stack);
        }
    }
    public static void main(String[] args) throws Exception {

        // create a CharStream that reads from standard input
        CharStream chars = CharStreams.fromStream(System.in);

        // create a lexer that feeds off of input CharStream
        LittleLexer lexer = new LittleLexer(chars);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        
        
        // create a parser that feeds off the tokens buffer
        LittleParser parser = new LittleParser(tokens);

        // walking the parse tree in order to print outputss
        parser.removeErrorListeners();
        parser.addErrorListener(new VerboseListener());
        parser.program();

        if (parser.getNumberOfSyntaxErrors() == 0) 
        	System.out.println("Accepted");
        if (parser.getNumberOfSyntaxErrors() != 0) 
        	System.out.println("Not accepted");
        }
    }
