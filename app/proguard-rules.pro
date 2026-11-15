# Xposed entry point: named in META-INF/xposed/java_init.list and loaded by reflection.
-keep class io.github.chalicechore.moduletemplate.xposed.ModuleMain { *; }
# libxposed API is provided by the framework at runtime.
-dontwarn io.github.libxposed.api.**
# TEMPLATE: keep every class whose methods are hook callbacks or are found by reflection.
