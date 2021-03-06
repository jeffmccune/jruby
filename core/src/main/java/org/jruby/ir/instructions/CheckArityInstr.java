package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.runtime.ThreadContext;

public class CheckArityInstr extends Instr implements FixedArityInstr {
    public final int required;
    public final int opt;
    public final int rest;
    public final boolean receivesKeywords;
    public final int restKey;

    public CheckArityInstr(int required, int opt, int rest, boolean receivesKeywords, int restKey) {
        super(Operation.CHECK_ARITY, EMPTY_OPERANDS);

        this.required = required;
        this.opt = opt;
        this.rest = rest;
        this.receivesKeywords = receivesKeywords;
        this.restKey = restKey;
    }

    @Override
    public String toString() {
        return super.toString() + " req: " + required + ", opt: " + opt + ", *r: " + rest + ", kw: " + receivesKeywords + ", **r: " + restKey;
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return new CheckArityInstr(required, opt, rest, receivesKeywords, restKey);

        InlineCloneInfo ii = (InlineCloneInfo) info;
        if (ii.canMapArgsStatically()) { // we can error on bad arity or remove check_arity
            int numArgs = ii.getArgsCount();

            if (numArgs < required || (rest == -1 && numArgs > (required + opt))) {
                return new RaiseArgumentErrorInstr(required, opt, rest, rest);
            }

            return null;
        }

        return new CheckArgsArrayArityInstr(ii.getArgs(), required, opt, rest);
    }

    public void checkArity(ThreadContext context, Object[] args) {
        IRRuntimeHelpers.checkArity(context, args, required, opt, rest, receivesKeywords, restKey);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.CheckArityInstr(this);
    }
}
