package io.syndesis.qe.hooks;

import io.syndesis.qe.utils.ExcludeFromSelectorReports;

import cucumber.api.event.EventListener;
import cucumber.api.event.EventPublisher;
import cucumber.api.java.en.Given;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

@Slf4j
public class ClassTransformerHook implements EventListener {

    private boolean shouldLoadAgent = true;

    public ClassTransformerHook() {
        transform();
    }

    private void transformSelectors() {
        lazyAgentInstall();
        /*
         The resulting agent annotates all methods loaded from apicurito testsuite with @ExcludeFromSelectorReports
         */
        new AgentBuilder.Default()
            .type(ElementMatchers.nameStartsWithIgnoreCase("apicurito.tests"))
            .transform(new AgentBuilder.Transformer() {
                @Override
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader,
                    JavaModule module) {
                    if (ElementMatchers
                        .isAnnotatedWith(new TypeDescription.ForPackageDescription(new PackageDescription.ForLoadedPackage(Given.class.getPackage())))
                        .matches(typeDescription)) {
                        log.debug("Transforming {}", typeDescription.getDeclaredMethods());
                    }
                    return builder
                        .method(ElementMatchers.isAnnotatedWith(
                            new TypeDescription.ForPackageDescription(new PackageDescription.ForLoadedPackage(Given.class.getPackage()))))
                        .intercept(SuperMethodCall.INSTANCE)
                        .annotateMethod(AnnotationDescription.Builder.ofType(ExcludeFromSelectorReports.class).build());
                }
            }).installOnByteBuddyAgent();
        /*
        The resulting agent changes methods annotated with @ExcludeFromSelectorReports to call ReporterPauseInterceptor#onEnter()
        And to call ReporterPauseInterceptor#onExit()
        TLDR: before the actual method is executed SelectorSnooper#pauseReporting() is called and SelectorSnooper#resumeReporting() is called after
         the method finishes
         */
        new AgentBuilder.Default()
            .with(AgentBuilder.PoolStrategy.Eager.EXTENDED)
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .type(ElementMatchers.nameContainsIgnoreCase("syndesis"))
            .transform(new AgentBuilder.Transformer() {
                @Override
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader,
                    JavaModule module) {
                    if (typeDescription.getDeclaredMethods().stream()
                        .anyMatch(inDefinedShape -> inDefinedShape.getDeclaredAnnotations().isAnnotationPresent(ExcludeFromSelectorReports.class))) {
                        log.debug("Transforming {}", typeDescription);
                    }
                    return builder
                        .method(ElementMatchers.isAnnotatedWith(ExcludeFromSelectorReports.class))
                        .intercept(Advice.to(ReporterPauseInterceptor.class));
                }
            }).installOnByteBuddyAgent();
    }

    private void transform() {
        if (true) {
            transformSelectors();
        }
    }

    ///Install Bytebuddy agent only once
    private void lazyAgentInstall() {
        if (shouldLoadAgent) {
            ByteBuddyAgent.install();
            shouldLoadAgent = false;
        }
    }

    public static class ReporterPauseInterceptor {

        @Advice.OnMethodEnter
        public static void onEnter() {
            SelectorSnooper.pauseReporting();
        }

        @Advice.OnMethodExit
        public static void onExit() {
            SelectorSnooper.resumeReporting();
        }
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        //NOOP all action is handled in constructor
    }
}
