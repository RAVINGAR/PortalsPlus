package com.ravingarinc.portals.api

/**
 * Annotates executing code as thread-safe, meaning it can be called from a context that is either synchronous or
 * asynchronous. If any particular method does not have this annotation, it may not be safe to call from an
 * asynchronous context.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.EXPRESSION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@MustBeDocumented
annotation class AsyncSafe()

/**
 * Annotates executing code as sync only. This means the corresponding code should only ever be called from the server's
 * main thread (aka only ever synchronously). If the executing code is executed asynchronously
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.EXPRESSION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@MustBeDocumented
annotation class SyncOnly()
