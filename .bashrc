# .bashrc

# Source global definitions
if [ -f /etc/bashrc ]; then
	. /etc/bashrc
fi

# User specific aliases and functions
export CLASSPATH='/home/10/n01467310/antlr-4.9.3-complete.jar:/home/10/n01467310/step1:$CLASSPATH'
alias antlr4='java -jar /home/10/n01467310/antlr-4.9.3-complete.jar'
alias grun='java org.antlr.v4.gui.TestRig'
