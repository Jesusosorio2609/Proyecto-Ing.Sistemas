package is.generador.core.domain.element;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record UmlNamespace(List<String> segments) {

    public UmlNamespace {
        Objects.requireNonNull(segments, "segments");
        segments = segments.stream().map(UmlNamespace::validSegment).toList();
    }

    public static UmlNamespace root() {
        return new UmlNamespace(List.of());
    }

    public static UmlNamespace of(List<String> segments) {
        return new UmlNamespace(segments);
    }

    public static UmlNamespace of(String... segments) {
        return new UmlNamespace(Arrays.asList(segments));
    }

    public static UmlNamespace fromQualifiedName(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");

        String normalizedName = qualifiedName.trim();

        if (normalizedName.isEmpty()) {
            return root();
        }

        return of(normalizedName.split("\\."));
    }

    public boolean isRoot() {
        return segments.isEmpty();
    }

    public UmlNamespace child(String segment) {
        var result = new java.util.ArrayList<>(segments);
        result.add(validSegment(segment));
        return new UmlNamespace(result);
    }

    public String qualifiedNameOf(String elementName) {
        var name = validSegment(elementName);
        return isRoot() ? name : this + "." + name;
    }

    @Override
    public String toString() {
        return String.join(".", segments);
    }

    private static String validSegment(String value) {
        Objects.requireNonNull(value, "namespace segment");
        var trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.contains(".")) {
            throw new IllegalArgumentException("Invalid namespace segment: " + value);
        }
        return trimmed;
    }
}
