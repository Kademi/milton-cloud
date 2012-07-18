
package io.milton.cloud.process;

public class TrueRule extends  AbstractRule {
    
    private static final long serialVersionUID = 1L;

    public TrueRule() {
    }
    
    @Override
    public boolean eval(ProcessContext context) {
        return true;
    }


}
