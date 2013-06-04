/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.process;

/**
 *
 * @author brad
 */
public class ComparisonRule implements Rule {

    public enum Operator {
        EQUALS,
        LESS_THEN,
        GREATER_THEN
    }
    private Operator operator;
    private Expression leftExpr;
    private Expression rightExpr;

    public ComparisonRule(Operator operator, Expression leftExpr, Expression rightExpr) {
        this.operator = operator;
        this.leftExpr = leftExpr;
        this.rightExpr = rightExpr;
    }
    
    
    @Override
    public Boolean eval(ProcessContext context) {
        Object leftVal = leftExpr.eval(context);
        Object rightVal = rightExpr.eval(context);
        return doComparison(leftVal, rightVal);
    }

    private Boolean doComparison(Object leftVal, Object rightVal) {
        if (leftVal instanceof Float && rightVal instanceof Float) {
            Float left = (Float) leftVal;
            Float right = (Float) rightVal;
            return fpComparison(left, right);
        } else {
            if (leftVal instanceof Comparable && rightVal instanceof Comparable) {
                Comparable left = (Comparable) leftVal;
                Comparable right = (Comparable) rightVal;
                return doComparison(left, right);
            } else {
                String left = leftVal.toString();
                String right = rightVal.toString();
                return doComparison(left, right);
            }
        }
    }

    private Boolean fpComparison(Float left, Float right) {
        int i = left.compareTo(right);
        switch (operator) {
            case EQUALS:
                return i == 0;
            case GREATER_THEN:
                return i > 0;
            case LESS_THEN:
                return i < 0;
            default:
                throw new RuntimeException();
        }
    }

    private Boolean doComparison(Comparable left, Comparable right) {
        int i = left.compareTo(right);
        switch (operator) {
            case EQUALS:
                return i == 0;
            case GREATER_THEN:
                return i > 0;
            case LESS_THEN:
                return i < 0;
            default:
                throw new RuntimeException();
        }
    }
}
