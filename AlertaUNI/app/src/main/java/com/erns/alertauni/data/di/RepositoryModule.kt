package com.erns.alertauni.data.di

import com.erns.alertauni.data.repository.AccountRepository
import com.erns.alertauni.data.repository.AccountRepositoryImpl
import com.erns.alertauni.data.repository.AnnounceRepository
import com.erns.alertauni.data.repository.AnnounceRepositoryImpl
import com.erns.alertauni.data.repository.AuthRepository
import com.erns.alertauni.data.repository.AuthRepositoryImpl
import com.erns.alertauni.data.repository.CommentRepository
import com.erns.alertauni.data.repository.CommentRepositoryImpl
import com.erns.alertauni.data.repository.ContactRepository
import com.erns.alertauni.data.repository.ContactRepositoryImpl
import com.erns.alertauni.data.repository.CourseRepository
import com.erns.alertauni.data.repository.CourseRepositoryImpl
import com.erns.alertauni.data.repository.PostRepository
import com.erns.alertauni.data.repository.PostRepositoryImpl
import com.erns.alertauni.data.repository.StudentRepository
import com.erns.alertauni.data.repository.StudentRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPostRepository(
        postRepositoryImpl: com.erns.alertauni.data.demo.FakePostRepository
    ): PostRepository

    @Binds
    @Singleton
    abstract fun bindCommentRepository(
        commentRepositoryImpl: com.erns.alertauni.data.demo.FakeCommentRepository
    ): CommentRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: com.erns.alertauni.data.demo.FakeAuthRepository
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        accountRepositoryImpl: com.erns.alertauni.data.demo.FakeAccountRepository
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindCourseRepository(
        courseRepositoryImpl: com.erns.alertauni.data.demo.FakeCourseRepository
    ): CourseRepository

    @Binds
    @Singleton
    abstract fun bindStudentRepository(
        studentRepositoryImpl: com.erns.alertauni.data.demo.FakeStudentRepository
    ): StudentRepository

    @Binds
    @Singleton
    abstract fun bindContactRepository(
        contactRepositoryImpl: com.erns.alertauni.data.demo.FakeContactRepository
    ): ContactRepository

    @Binds
    @Singleton
    abstract fun bindAnnounceRepository(
        announceRepositoryImpl: com.erns.alertauni.data.demo.FakeAnnounceRepository
    ): AnnounceRepository
}
