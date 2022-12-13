# .bash_profile

# Get the aliases and functions
if [ -f ~/.bashrc ]; then
	. ~/.bashrc
fi

# User specific environment and startup programs

PATH=$PATH:$HOME/bin

export PATH

alias antlr4='java -Xmx500M -cp "/home/10/n01467310/antlr-4.9.3-complete.jar:$CLASSPATH" org.antlr.v4.Tool'
alias grun='java -Xmx500M -cp "/home/10/n01467310/antlr-4.9.3-complete.jar:$CLASSPATH" org.antlr.v4.gui.TestRig'

