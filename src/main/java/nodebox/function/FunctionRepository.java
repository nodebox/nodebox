package nodebox.function;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import nodebox.node.Node;
import nodebox.node.Port;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages a collection of function libraries.
 */
public class FunctionRepository {

    public static FunctionRepository of(FunctionLibrary... libraries) {
        ImmutableSet.Builder<FunctionLibrary> librarySet = ImmutableSet.builder();
        librarySet.addAll(ImmutableSet.copyOf(libraries));
        // The core library is always included.
        librarySet.add(CoreFunctions.LIBRARY);

        ImmutableMap.Builder<String, FunctionLibrary> builder = ImmutableMap.builder();
        for (FunctionLibrary library : librarySet.build()) {
            builder.put(library.getNamespace(), library);
        }
        return new FunctionRepository(builder.build());
    }

    public static FunctionRepository combine(FunctionRepository... repositories) {
        ImmutableSet.Builder<FunctionLibrary> librarySet = ImmutableSet.builder();
        // The core library is always included.
        librarySet.add(CoreFunctions.LIBRARY);
        for (FunctionRepository repository : repositories) {
            librarySet.addAll(repository.getLibraries());
        }
        ImmutableMap.Builder<String, FunctionLibrary> builder = ImmutableMap.builder();
        for (FunctionLibrary library : librarySet.build()) {
            builder.put(library.getNamespace(), library);
        }
        return new FunctionRepository(builder.build());
    }

    private final ImmutableMap<String, FunctionLibrary> libraryMap;
    private final transient Map<String, Function> functionCache = new HashMap<String, Function>();

    private FunctionRepository(ImmutableMap<String, FunctionLibrary> libraryMap) {
        this.libraryMap = libraryMap;
    }

    public void reload() {
        invalidateFunctionCache();
        for (FunctionLibrary library : getLibraries()) {
            if (library == CoreFunctions.LIBRARY) continue;
            library.reload();
        }
    }

    public void invalidateFunctionCache() {
        functionCache.clear();
    }

    public Function getFunction(String identifier) {
        if (functionCache.containsKey(identifier)) {
            return functionCache.get(identifier);
        } else {
            String[] functionParts = identifier.split("/");
            checkArgument(functionParts.length == 2, "The function identifier should be in the form 'namespace/function'.");
            String namespace = functionParts[0];
            String functionName = functionParts[1];
            FunctionLibrary library = libraryMap.get(namespace);
            checkArgument(library != null, "Could not find function %s: unknown namespace.", identifier);
            assert library != null; // To avoid a compiler warning.
            checkArgument(library.hasFunction(functionName), "Could not find function %s: unknown function.", identifier);
            Function function = library.getFunction(functionName);
            functionCache.put(identifier, function);
            return function;
        }
    }

    public boolean hasFunction(String identifier) {
        try {
            return getFunction(identifier) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public Node nodeForFunction(String identifier) {
        Function function = getFunction(identifier);
        Node n = Node.ROOT.withFunction(identifier);
        for (Function.Argument arg : function.getArguments()) {
            n = n.withInputAdded(Port.portForType(arg.getName(), arg.getType()));
        }
        return n;
    }

    public Collection<FunctionLibrary> getLibraries() {
        return libraryMap.values();
    }

    public boolean hasLibrary(String namespace) {
        return libraryMap.containsKey(namespace);
    }

    public FunctionLibrary getLibrary(String namespace) {
        checkNotNull(namespace);
        checkArgument(libraryMap.containsKey(namespace), "Could not find library %s: unknown namespace.", namespace);
        return libraryMap.get(namespace);
    }

    public FunctionRepository withLibraryAdded(FunctionLibrary newLibrary) {
        List<FunctionLibrary> newLibraries = new ArrayList<FunctionLibrary>();

        Collection<FunctionLibrary> libraries = getLibraries();
        if (libraries.contains(newLibrary)) {
            for (FunctionLibrary library : libraries) {
                if (library.equals(newLibrary))
                    newLibraries.add(newLibrary);
                else
                    newLibraries.add(library);
            }
        } else {
            newLibraries.addAll(libraries);
            newLibraries.add(newLibrary);
        }

        FunctionLibrary[] fl = newLibraries.toArray(new FunctionLibrary[newLibraries.size()]);
        return FunctionRepository.of(fl);
    }

    public FunctionRepository withLibraryRemoved(FunctionLibrary library) {
        checkNotNull(library);
        checkArgument(hasLibrary(library.getNamespace()), "Could not find library %s: unknown namespace.", library.getNamespace());
        List<FunctionLibrary> newLibraries = new ArrayList<FunctionLibrary>();
        newLibraries.addAll(getLibraries());
        newLibraries.remove(library);
        FunctionLibrary[] fl = newLibraries.toArray(new FunctionLibrary[newLibraries.size()]);
        return FunctionRepository.of(fl);
    }

    //// Object overrides ////

    @Override
    public int hashCode() {
        return Objects.hashCode(libraryMap);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FunctionRepository)) return false;
        final FunctionRepository other = (FunctionRepository) o;
        return Objects.equal(libraryMap, other.libraryMap);
    }

    @Override
    public String toString() {
        return String.format("<FunctionRepository %s>", Joiner.on(", ").join(libraryMap.values()));
    }

}
