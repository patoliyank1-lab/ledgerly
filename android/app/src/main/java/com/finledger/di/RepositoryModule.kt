package com.finledger.di

import com.finledger.data.repository.AccountRepository
import com.finledger.data.repository.AccountRepositoryImpl
import com.finledger.data.repository.AuditLogRepository
import com.finledger.data.repository.AuditLogRepositoryImpl
import com.finledger.data.repository.CategoryRepository
import com.finledger.data.repository.CategoryRepositoryImpl
import com.finledger.data.repository.TagRepository
import com.finledger.data.repository.TagRepositoryImpl
import com.finledger.data.repository.TransactionRepository
import com.finledger.data.repository.TransactionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds @Singleton
    abstract fun bindAuditLogRepository(impl: AuditLogRepositoryImpl): AuditLogRepository
}

