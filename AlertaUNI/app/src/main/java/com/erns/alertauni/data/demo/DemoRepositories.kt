package com.erns.alertauni.data.demo

import com.erns.alertauni.data.model.BackendError
import com.erns.alertauni.data.model.BackendException
import com.erns.alertauni.data.model.CatalogCourseEntity
import com.erns.alertauni.data.model.ClassCodeRequest
import com.erns.alertauni.data.model.CommentDto
import com.erns.alertauni.data.model.CommentEntity
import com.erns.alertauni.data.model.CommentRequest
import com.erns.alertauni.data.model.CourseCatalogEntity
import com.erns.alertauni.data.model.CourseCatalogRequest
import com.erns.alertauni.data.model.CourseEnrollRequest
import com.erns.alertauni.data.model.CourseEnrollResponse
import com.erns.alertauni.data.model.GoogleAccountRequest
import com.erns.alertauni.data.model.PostAddResponse
import com.erns.alertauni.data.model.PostEntity
import com.erns.alertauni.data.model.PostPrivateRequest
import com.erns.alertauni.data.model.PostPrivateResponse
import com.erns.alertauni.data.model.PostRequest
import com.erns.alertauni.data.model.StudentEnrollment
import com.erns.alertauni.data.model.StudentEntity
import com.erns.alertauni.data.model.UserProfile
import com.erns.alertauni.data.model.UserProfileResponse
import com.erns.alertauni.data.repository.AccountRepository
import com.erns.alertauni.data.repository.AnnounceRepository
import com.erns.alertauni.data.repository.AuthRepository
import com.erns.alertauni.data.repository.CommentRepository
import com.erns.alertauni.data.repository.ContactRepository
import com.erns.alertauni.data.repository.CourseRepository
import com.erns.alertauni.data.repository.PostRepository
import com.erns.alertauni.data.repository.StudentRepository
import com.erns.alertauni.domain.manager.DataStoreHelper
import io.github.jan.supabase.gotrue.user.UserInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MODO DEMO (solo rama demo/local-run).
 * Reemplaza el backend Supabase por datos en memoria para poder ejecutar la app
 * sin credenciales reales. Ver docs/EJECUCION_Y_FUNCIONALIDADES.md.
 */
object DemoConfig {
    const val ENABLED = true
    const val EMAIL = "demo@alertauni.test"
    const val CODE_TO_FIND = "ABC123"
}

/** Almacen unico compartido por todos los repositorios falsos. */
@Singleton
class DemoStore @Inject constructor() {
    val courses = listOf(
        CatalogCourseEntity("cat-1", "1703133", "Introducción al Desarrollo de Nuevas Plataformas"),
        CatalogCourseEntity("cat-2", "1703121", "Ingeniería de Software"),
        CatalogCourseEntity("cat-3", "1703140", "Base de Datos II")
    )

    val posts = mutableListOf(
        PostEntity(
            id = 3, authorName = "Dra. Rosa Quispe", authorType = 1, courseCode = "1703133",
            courseName = "Introducción al Desarrollo de Nuevas Plataformas",
            title = "Examen 1 - 2026B", content = "Recuerden revisar el enunciado y formar grupos de 4 personas.",
            allowComments = true, countComments = 2, isPrivate = false,
            date = "27/09/2026", receiverPrivate = ""
        ),
        PostEntity(
            id = 2, authorName = "Dra. Rosa Quispe", authorType = 1, courseCode = "1703121",
            courseName = "Ingeniería de Software",
            title = "Entrega de avance", content = "El avance del proyecto se sube al aula virtual hasta el viernes.",
            allowComments = true, countComments = 1, isPrivate = false,
            date = "25/09/2026", receiverPrivate = ""
        ),
        PostEntity(
            id = 1, authorName = "Dr. Luis Mamani", authorType = 1, courseCode = "1703140",
            courseName = "Base de Datos II",
            title = "Clase de repaso", content = "El sábado habrá clase de repaso en el laboratorio 3.",
            allowComments = true, countComments = 0, isPrivate = true,
            date = "20/09/2026", receiverPrivate = "Ana Torres"
        )
    )

    val comments = mutableMapOf(
        3 to mutableListOf(
            CommentEntity(1, 3, "Ana Torres", "¿El informe es en PDF?", "ana@unsa.edu.pe", false, "27/09/2026 10:02"),
            CommentEntity(2, 3, "Dra. Rosa Quispe", "Sí, en PDF y con capturas.", "docente@unsa.edu.pe", false, "27/09/2026 10:15")
        ),
        2 to mutableListOf(
            CommentEntity(3, 2, "Carlos Ríos", "¿Se entrega por grupo?", "carlos@unsa.edu.pe", false, "25/09/2026 18:40")
        )
    )

    val enrollments = mutableListOf(
        enrollment("cat-1", "1703133", "Introducción al Desarrollo de Nuevas Plataformas", "A"),
        enrollment("cat-2", "1703121", "Ingeniería de Software", "B")
    )

    val students = listOf(
        StudentEntity("cat-1", "s1", "20230001", "Ana", "Torres", "ana@unsa.edu.pe"),
        StudentEntity("cat-1", "s2", "20230002", "Carlos", "Ríos", "carlos@unsa.edu.pe"),
        StudentEntity("cat-1", "s3", "20230003", "Lucía", "Flores", "lucia@unsa.edu.pe")
    )

    var nextPostId = 4
    var nextCommentId = 4

    fun enrollment(catalogId: String, code: String, name: String, group: String) = StudentEnrollment(
        course_catalog_id = catalogId, courseId = "c-$catalogId", courseCode = code, courseName = name,
        semester = "2026-B", courseType = "Teoría", groupType = group,
        firstname = "Rosa", surname = "Quispe", email = "docente@unsa.edu.pe"
    )
}

class FakeAuthRepository @Inject constructor(
    private val dataStore: DataStoreHelper
) : AuthRepository {
    override suspend fun signInSupaWithGoogle(googleIdToken: String): Result<UserInfo> =
        Result.failure(UnsupportedOperationException("Login real deshabilitado en modo demo"))

    override suspend fun checkProfile(): Result<UserProfile> =
        Result.success(UserProfile(DemoConfig.EMAIL, dataStore.getUserType() ?: "STUDENT"))

    override suspend fun updateProfile(googleAccountRequest: GoogleAccountRequest): Result<UserProfileResponse> =
        Result.success(UserProfileResponse(DemoConfig.EMAIL, 1, "2026-09-27"))
}

class FakeAccountRepository @Inject constructor() : AccountRepository {
    override suspend fun createAccountWithGoogle(googleAccountRequest: GoogleAccountRequest) = Result.success(Unit)
    override suspend fun checkProfile() = Result.success(true)
}

class FakePostRepository @Inject constructor(
    private val store: DemoStore,
    private val dataStore: DataStoreHelper
) : PostRepository {
    override suspend fun sendPost(postRequest: PostRequest): Result<PostAddResponse> {
        val course = store.courses.first { it.id == postRequest.course_catalog_id }
        val id = store.nextPostId++
        store.posts.add(
            0, PostEntity(
                id = id, authorName = dataStore.getFirstname() + " " + dataStore.getSurname(),
                authorType = 1, courseCode = course.courseCode, courseName = course.courseName,
                title = postRequest.title, content = postRequest.content,
                allowComments = postRequest.allow_comments, countComments = 0,
                isPrivate = postRequest.is_private, date = "28/09/2026", receiverPrivate = ""
            )
        )
        return Result.success(PostAddResponse(id, "2026-09-28"))
    }

    // Igual que PostRepositoryImpl: lista vacia => Failure("No posts found")
    override suspend fun getPosts(): Result<List<PostEntity>> =
        if (store.posts.isNotEmpty()) Result.success(store.posts.toList())
        else Result.failure(Exception("No posts found"))

    override suspend fun getCourseCatalog(): Result<List<CatalogCourseEntity>> =
        Result.success(store.courses)
}

class FakeCommentRepository @Inject constructor(
    private val store: DemoStore,
    private val dataStore: DataStoreHelper
) : CommentRepository {
    override suspend fun sendComment(commentRequest: CommentRequest): Result<Boolean> {
        val postId = commentRequest.post_id.toInt()
        val list = store.comments.getOrPut(postId) { mutableListOf() }
        list.add(
            CommentEntity(
                store.nextCommentId++, postId, dataStore.getFirstname() + " " + dataStore.getSurname(),
                commentRequest.content, DemoConfig.EMAIL, true, "28/09/2026"
            )
        )
        val i = store.posts.indexOfFirst { it.id == postId }
        if (i >= 0) store.posts[i] = store.posts[i].copy(countComments = list.size)
        return Result.success(true)
    }

    // Igual que CommentRepositoryImpl: sin comentarios => Failure("No comments found")
    override suspend fun getComments(commentDto: CommentDto): Result<List<CommentEntity>> {
        val list = store.comments[commentDto.post_id].orEmpty()
        if (list.isEmpty()) return Result.failure(Exception("No comments found"))
        val me = dataStore.getEmail()
        return Result.success(list.map { it.copy(isMine = it.email == me) })
    }
}

class FakeCourseRepository @Inject constructor(
    private val store: DemoStore
) : CourseRepository {
    override suspend fun getCourseCatalogList() =
        Result.success(store.courses.map { CourseCatalogEntity(it.id, it.courseCode, it.courseName) })
}

class FakeStudentRepository @Inject constructor(
    private val store: DemoStore
) : StudentRepository {
    override suspend fun getStudentEnrollment() = Result.success(store.enrollments.toList())

    override suspend fun findCourse(classCode: ClassCodeRequest): Result<StudentEnrollment> =
        if (classCode.classCode.equals(DemoConfig.CODE_TO_FIND, ignoreCase = true))
            Result.success(store.enrollment("cat-3", "1703140", "Base de Datos II", "A"))
        else Result.failure(Exception("Curso no encontrado"))

    override suspend fun courseEnroll(courseEnrollRequest: CourseEnrollRequest): Result<CourseEnrollResponse> {
        if (store.enrollments.none { it.course_catalog_id == courseEnrollRequest.courseCatalogId }) {
            store.enrollments.add(store.enrollment("cat-3", "1703140", "Base de Datos II", "A"))
        }
        return Result.success(CourseEnrollResponse("2026-09-28"))
    }

    override suspend fun courseStudent(courseCatalogRequest: CourseCatalogRequest) =
        Result.success(store.students)
}

class FakeContactRepository @Inject constructor(
    private val store: DemoStore
) : ContactRepository {
    override suspend fun getStudentsByCourse(courseCatalogRequest: CourseCatalogRequest) =
        Result.success(store.students)
}

class FakeAnnounceRepository @Inject constructor(
    private val store: DemoStore,
    private val dataStore: DataStoreHelper
) : AnnounceRepository {
    override suspend fun addPostPrivate(postPrivateRequest: PostPrivateRequest): Result<PostPrivateResponse> {
        val course = store.courses.first { it.id == postPrivateRequest.courseCatalogId }
        store.posts.add(
            0, PostEntity(
                id = store.nextPostId++, authorName = dataStore.getFirstname() + " " + dataStore.getSurname(),
                authorType = 1, courseCode = course.courseCode, courseName = course.courseName,
                title = postPrivateRequest.title, content = postPrivateRequest.content,
                allowComments = postPrivateRequest.allowComments, countComments = 0,
                isPrivate = true, date = "28/09/2026", receiverPrivate = postPrivateRequest.receiverPrivate
            )
        )
        return Result.success(PostPrivateResponse("2026-09-28"))
    }

    override suspend fun addPostPublic(postRequest: PostRequest): Result<PostPrivateResponse> =
        Result.success(PostPrivateResponse("2026-09-28"))
}
