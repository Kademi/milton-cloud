package com.ettrema.db.types;

/**
 *
 * @author brad
 */
public class FieldTypes {

    public final static CharacterVaryingType CHARACTER_VARYING = new CharacterVaryingType();
    public final static BinaryType BINARY = new BinaryType();
    public final static IntegerType INTEGER = new IntegerType();
    public final static LongType LONG = new LongType();
    public final static TimestampType TIMESTAMP = new TimestampType();
    public final static Float8Type FLOAT8 = new Float8Type();
    public final static FieldType[] allTypes = {
        CHARACTER_VARYING,
        BINARY,
        INTEGER,
        LONG,
        TIMESTAMP,
        FLOAT8
    };

    public static CharacterType character(int length) {
        return new CharacterType(length);
    }

    public static FieldType fromName(String name) {
        for (FieldType type : allTypes) {
            if (type.toString().equals(name)) {
                return type;
            }
        }
        return null;
    }
}
