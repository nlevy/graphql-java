package graphql.schema.idl;

import graphql.Internal;
import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLType;

import java.util.Stack;

/**
 * This helper gives you access to the type info given a type definition
 */
@Internal
public class TypeInfo {

    public static TypeInfo typeInfo(Type type) {
        return new TypeInfo(type);
    }

    private final Type rawType;
    private final TypeName typeName;
    private final Stack<Class<?>> decoration = new Stack<>();

    public TypeInfo(Type type) {
        this.rawType = type;
        while (!(type instanceof TypeName)) {
            if (type instanceof NonNullType) {
                decoration.push(NonNullType.class);
                type = ((NonNullType) type).getType();
            }
            if (type instanceof ListType) {
                decoration.push(ListType.class);
                type = ((ListType) type).getType();
            }
        }
        this.typeName = (TypeName) type;
    }

    public Type getRawType() {
        return rawType;
    }

    public TypeName getTypeName() {
        return typeName;
    }

    public String getName() {
        return typeName.getName();
    }

    public boolean isList() {
        return rawType instanceof ListType;
    }

    public boolean isNonNull() {
        return rawType instanceof NonNullType;
    }

    public boolean isPlain() {
        return !isList() && !isNonNull();
    }

    /**
     * This will decorate a graphql type with the original hierarchy of non null and list'ness
     * it originally contained in its definition type
     *
     * @param objectType this should be a graphql type that was originally built from this raw type
     * @param <T>        the type
     *
     * @return the decorated type
     */
    public <T extends GraphQLType> T decorate(GraphQLType objectType) {

        GraphQLType out = objectType;
        Stack<Class<?>> wrappingStack = new Stack<>();
        wrappingStack.addAll(this.decoration);
        while (!wrappingStack.isEmpty()) {
            Class<?> clazz = wrappingStack.pop();
            if (clazz.equals(NonNullType.class)) {
                out = new GraphQLNonNull(out);
            }
            if (clazz.equals(ListType.class)) {
                out = new GraphQLList(out);
            }
        }
        // we handle both input and output graphql types
        //noinspection unchecked
        return (T) out;
    }

    public static String getAstDesc(Type type) {
        if (type instanceof NonNullType) {
            return getAstDesc(((NonNullType) type).getType()) + "!";
        }
        if (type instanceof ListType) {
            return "[" + getAstDesc(((ListType) type).getType()) + "]";
        }
        return ((TypeName) type).getName();
    }

    public TypeInfo unwrapOne() {
        if (rawType instanceof NonNullType) {
            return typeInfo(((NonNullType) rawType).getType());
        }
        if (rawType instanceof ListType) {
            return typeInfo(((ListType) rawType).getType());
        }
        return this;
    }

    @Override
    public String toString() {
        return "TypeInfo{" +
                "rawType=" + rawType +
                ", typeName=" + typeName +
                ", isNonNull=" + decoration +
                '}';
    }
}

