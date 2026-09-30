package is.generador.core.ports;

/**
 * Proporciona las políticas de clasificación del dominio sin acoplar el
 * núcleo a un lenguaje o plataforma concretos.
 */
public interface DomainPolicyProvider {

    boolean isAllowedExternalType(String fqn);

    boolean isLanguageNativeType(String fqn);

    boolean isStructural(String fqn);

    boolean isBlacklisted(String fqn);

    boolean isStandardLibrary(String fqn);
}
