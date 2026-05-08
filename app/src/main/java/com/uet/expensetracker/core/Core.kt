package com.uet.expensetracker.core

import com.uet.expensetracker.core.di.CoreModule
import dagger.Module

/**
 * The fundamental piece of modularity.
 *
 * An Application always contains features,
 * and that is represented by this
 * contract/abstraction.
 */
interface Feature {

    /**
     * Convention: The feature name should follow
     * the package name where the feature is contained
     * using lower case.
     *
     * Examples:
     *  - core
     *  - login
     *  - movies
     */
    fun name(): String

    /**
     * Module that will be included
     * when creating the Dependency Graph
     * at Application Startup.
     *
     * In case of scoping this will facilitate
     * refactor.
     */
    fun diModule(): Module

    /**
     * LEARNING PURPOSE:
     * In order to keep modularity, each feature could
     * point to the tables in the database related
     * to the feature itself.
     */
    // fun databaseTables(): List<Table> = emptyList()
}

private fun coreFeature() = object : Feature {
    override fun name() = "core"
    override fun diModule() = CoreModule as Module
}

private fun allFeatures() = listOf(
    coreFeature()
)