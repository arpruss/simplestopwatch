package omegacentauri.mobi.simplestopwatch;

import java.util.HashMap;
import java.util.LinkedList;

public class FormatTime {
    HashMap<String,Token> variables;
    LinkedList<Token> values = new LinkedList<>();
    LinkedList<Token> code = new LinkedList<>();
    enum Operator {
        PLUS,
        MINUS,
        UNARYMINUS,
        TIMES,
        DIVIDE,
        MODULO,
        POWER,
        AND,
        OR,
        NOT,
        EQUALS,
        LET,
        IFELSE,
        FORMAT,
        FORMATLEFT,
        LT,
        LE,
        GT,
        GE
    }

    public static void main(String[] args) {
        System.out.println("Hello world!");
    }

    public class Token {
        public String toText() {
            return "";
        }
        public long toNumber() {
            return 0;
        }
        public void step(LinkedList<Token> stack) {
            stack.add(this);
        }
    }

    public class StringToken extends Token {
        String mValue;

        public StringToken(String value) {
            mValue = value;
        }

        @Override
        public String toText() {
            return mValue;
        }

        @Override
        public long toNumber() {
            try {
                return Long.parseLong(mValue);
            }
            catch(NumberFormatException e) {
                return 0;
            }
        }
    }

    public class NumberToken extends Token {
        long mValue;

        public NumberToken(long value) {
            mValue = value;
        }

        @Override
        public String toText() {
            return Long.toString(mValue);
        }

        @Override
        public long toNumber() {
            return mValue;
        }

    }

    public class UndefinedToken extends Token {
        @Override
        public String toText() {
            return "";
        }

        @Override
        public long toNumber() {
            return 0;
        }
    }

    public class LinkedListToken extends Token {
        LinkedList<Token> list;

        public LinkedListToken(LinkedList<Token> s) {
            list = s;
        }

        @Override
        public String toText() {
            String out = "";
            for (Token t: list) {
                out += t.toText();
            }
            return out;
        }

        @Override
        public long toNumber() {
            try {
                return list.getLast().toNumber();
            }
            catch(Exception e) {
                return 0;
            }
        }

        public void step(LinkedList<Token> stack) {
            stack.add(this);
        }

        public void run(LinkedList<Token> stack) {
            for (Token token: list) {
                token.step(stack);
            }
        }
    }

    public class VariableToken extends Token {
        String mName;
        Token currentValue;

        public VariableToken(String name) {
            mName = name;
            currentValue = variables.get(name);
        }

        public Token get() {
            if (currentValue == null)
                return new UndefinedToken();
            else
                return currentValue;
        }

        @Override
        public String toText() {
            return currentValue.toText();
        }

        @Override
        public long toNumber() {
            return currentValue.toNumber();
        }

        public void let(Token x) {
            variables.put(mName, x);
        }
    }

    public class OperatorToken extends Token {
        Operator op;

        public OperatorToken(Operator _op) {
            op = _op;
        }

        private Token pop(LinkedList<Token> l) {
            Token x = l.getLast();
            l.removeLast();
            return x;
        }

        public void step(LinkedList<Token> stack) {
            Token x;
            Token y;
            Token z;
            long value;
            String pad;
            long digits;
            String out;

            switch(op) {
                case PLUS:
                    y = pop(stack);
                    x = pop(stack);
                    stack.add(new NumberToken(x.toNumber()+y.toNumber()));
                    break;
                case MINUS:
                    y = pop(stack);
                    x = pop(stack);
                    stack.add(new NumberToken(x.toNumber()-y.toNumber()));
                    break;
                case UNARYMINUS:
                    stack.add(new NumberToken(-pop(stack).toNumber()));
                    break;
                case TIMES:
                    y = pop(stack);
                    x = pop(stack);
                    stack.add(new NumberToken(x.toNumber()*y.toNumber()));
                    break;
                case DIVIDE:
                    y = pop(stack);
                    x = pop(stack);
                    try {
                        stack.add(new NumberToken(x.toNumber() * y.toNumber()));
                    }
                    catch(Exception e) {
                        stack.add(new UndefinedToken());
                    }
                    break;
                case MODULO:
                    y = pop(stack);
                    x = pop(stack);
                    try {
                        stack.add(new NumberToken(x.toNumber() / y.toNumber()));
                    }
                    catch(Exception e) {
                        stack.add(new UndefinedToken());
                    }
                    break;
                case POWER:
                    y = pop(stack);
                    x = pop(stack);
                    try {
                        stack.add(new NumberToken((long) Math.pow(x.toNumber(),y.toNumber())));
                    }
                    catch(Exception e) {
                        stack.add(new UndefinedToken());
                    }
                    break;
                case AND:
                    y = pop(stack);
                    x = pop(stack);
                    stack.add(new NumberToken((x.toNumber() != 0  && y.toNumber() != 0) ? 1 : 0));
                    break;
                case OR:
                    y = pop(stack);
                    x = pop(stack);
                    stack.add(new NumberToken((x.toNumber() != 0  || y.toNumber() != 0) ? 1 : 0));
                    break;
                case NOT:
                    x = pop(stack);
                    stack.add(new NumberToken((x.toNumber() != 0) ? 0 : 1));
                    break;
                case EQUALS:
                    y = pop(stack);
                    x = pop(stack);
                    stack.add(new NumberToken((x.toNumber() == y.toNumber()) ? 1 : 0));
                    break;
                case LT:
                    y = pop(stack);
                    x = pop(stack);
                    stack.add(new NumberToken((x.toNumber() < y.toNumber()) ? 1 : 0));
                    break;
                case LE:
                    y = pop(stack);
                    x = pop(stack);
                    stack.add(new NumberToken((x.toNumber() <= y.toNumber()) ? 1 : 0));
                    break;
                case GT:
                    y = pop(stack);
                    x = pop(stack);
                    stack.add(new NumberToken((x.toNumber() > y.toNumber()) ? 1 : 0));
                    break;
                case GE:
                    y = pop(stack);
                    x = pop(stack);
                    stack.add(new NumberToken((x.toNumber() >= y.toNumber()) ? 1 : 0));
                    break;
                case LET:
                    y = pop(stack);
                    x = pop(stack);
                    if (y instanceof VariableToken) {
                        VariableToken v = (VariableToken)y;
                        v.let(x);
                    }
                    break;
                case IFELSE:
                    z = pop(stack);
                    y = pop(stack);
                    x = pop(stack);
                    Token go = z.toNumber() != 0 ? y : x;
                    if (go instanceof  LinkedListToken) {
                        LinkedListToken t = (LinkedListToken) go;
                        t.run(stack);
                    }
                    else {
                        stack.add(new StringToken(go.toText()));
                    }
                    break;
                case FORMAT:
                    pad = pop(stack).toText(); // pad
                    if (pad.length() == 0)
                        pad = " ";
                    digits = pop(stack).toNumber(); // digits
                    value = pop(stack).toNumber(); // value
                    out = Long.toString(value);
                    while (out.length() < digits) {
                        out = pad + out;
                    }
                    stack.add(new StringToken(out));
                    break;
                case FORMATLEFT:
                    pad = pop(stack).toText(); // pad
                    if (pad.length() == 0)
                        pad = " ";
                    digits = pop(stack).toNumber(); // digits
                    value = pop(stack).toNumber(); // value
                    out = Long.toString(value);
                    while (out.length() < digits) {
                        out = out + pad;
                    }
                    stack.add(new StringToken(out));
                    break;
            }
        }
    }
}
