plugins {
    // 引用 libs.versions.toml 中定义的插件
    // apply false 表示不自动应用到根项目,只在子模块中按需使用
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktor) apply false
}

allprojects {
    group = "com.ktormart"
    version = "0.0.1"

    repositories {
        mavenCentral()
    }
}
