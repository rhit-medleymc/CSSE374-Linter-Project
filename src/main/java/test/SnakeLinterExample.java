package test;

public class SnakeLinterExample {
    private int FailingVarriable;
    private double good_varriable;
    private String OTHER_BAD_VARRIABLE;

    public SnakeLinterExample(){
        FailingVarriable = 5;
        good_varriable = 6.5;
        OTHER_BAD_VARRIABLE = "la la la";
        float badVarInFunction = (float)4.5;
        bad_function_name(badVarInFunction);
    }

    private void bad_function_name(float good){
        float Bad = good;
        int good_var = goodFunctionName(Bad);
    }

    private int goodFunctionName(float Bad){
        int also_good = 5;
        return also_good;
    }
}

