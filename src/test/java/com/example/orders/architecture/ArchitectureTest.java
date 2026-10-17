package com.example.orders.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the spec-first architecture (constitution, Article II). Uses ArchUnit
 * from plain JUnit tests, so no extra test engine is needed.
 */
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.example.orders");
    }

    @Test
    void controllersImplementGeneratedApiInterfaces() {
        classes().that().areAnnotatedWith(RestController.class)
                .should().implement(resideInAPackage("com.example.orders.api"))
                .because("every endpoint must come from api/openapi.yaml (Article II.2)")
                .check(classes);
    }

    @Test
    void controllersDoNotDeclareTheirOwnMappings() {
        methods().that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .should().notBeAnnotatedWith(RequestMapping.class)
                .andShould().notBeAnnotatedWith(GetMapping.class)
                .andShould().notBeAnnotatedWith(PostMapping.class)
                .andShould().notBeAnnotatedWith(PutMapping.class)
                .andShould().notBeAnnotatedWith(PatchMapping.class)
                .andShould().notBeAnnotatedWith(DeleteMapping.class)
                .because("request mappings come from the generated interface, not hand-written code")
                .check(classes);

        classes().that().areAnnotatedWith(RestController.class)
                .should().notBeAnnotatedWith(RequestMapping.class)
                .because("request mappings come from the generated interface, not hand-written code")
                .check(classes);
    }

    @Test
    void domainIsFreeOfFrameworkAndContractTypes() {
        noClasses().that().resideInAPackage("com.example.orders.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.servlet..",
                        "com.fasterxml.jackson..",
                        "tools.jackson..",
                        "com.example.orders.api..",
                        "com.example.orders.web..",
                        "com.example.orders.infrastructure..")
                .because("business rules must not depend on the web layer or generated DTOs (Article II.3)")
                .check(classes);
    }

    @Test
    void generatedDtosStayInTheWebLayer() {
        noClasses().that().resideOutsideOfPackages(
                        "com.example.orders.api..",
                        "com.example.orders.web..")
                .should().dependOnClassesThat().resideInAPackage("com.example.orders.api..")
                .because("generated contract types are mapped to domain types at the web boundary")
                .check(classes);
    }

    @Test
    void webLayerUsesServicesNotStorage() {
        noClasses().that().resideInAPackage("com.example.orders.web..")
                .should().dependOnClassesThat().resideInAPackage("com.example.orders.infrastructure..")
                .orShould().dependOnClassesThat().haveSimpleName("OrderRepository")
                .because("controllers go through OrderService")
                .check(classes);
    }
}
