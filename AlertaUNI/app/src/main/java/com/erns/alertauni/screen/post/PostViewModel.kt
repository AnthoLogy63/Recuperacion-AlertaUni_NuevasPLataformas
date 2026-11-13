package com.erns.alertauni.screen.post

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erns.alertauni.data.model.BackendException
import com.erns.alertauni.data.model.PostEntity
import com.erns.alertauni.data.model.CatalogCourseEntity
import com.erns.alertauni.data.model.PostRequest
import com.erns.alertauni.data.repository.PostRepository
import com.erns.alertauni.domain.manager.DataStoreHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val dataStoreHelper: DataStoreHelper
) : ViewModel() {
    private val TAG = "PostViewModel"

    sealed class PostState {
        object Loading : PostState()
        object Saved : PostState()
        object UnSaved : PostState()
    }

    private val _postState = MutableStateFlow<PostState>(PostState.Loading)
    val postState: StateFlow<PostState> = _postState
    private val _announcements = MutableStateFlow<List<PostEntity>>(emptyList())
    val announcements: StateFlow<List<PostEntity>> = _announcements
    private val _highlightedPostId = MutableStateFlow<Int?>(null)
    val highlightedPostId: StateFlow<Int?> = _highlightedPostId

    private val _catalogCourses = MutableStateFlow<List<CatalogCourseEntity>>(emptyList())
    val catalogCourses: StateFlow<List<CatalogCourseEntity>> = _catalogCourses

    // Criterios de busqueda y filtros (estado de la pantalla; sobreviven a la navegacion)
    private val _query = MutableStateFlow(com.erns.alertauni.screen.proposal.ProposalConfig.p2Query)
    val query: StateFlow<String> = _query
    private val _course = MutableStateFlow(com.erns.alertauni.screen.proposal.ProposalConfig.p2Course)
    val course: StateFlow<String> = _course
    private val _vis = MutableStateFlow(com.erns.alertauni.screen.proposal.ProposalConfig.p2Vis)
    val vis: StateFlow<String> = _vis
    private val _sort = MutableStateFlow(com.erns.alertauni.screen.proposal.ProposalConfig.p2Sort)
    val sort: StateFlow<String> = _sort
    fun setQuery(v: String) { _query.value = v }
    fun setCourse(v: String) { _course.value = v }
    fun setVis(v: String) { _vis.value = v }
    fun setSort(v: String) { _sort.value = v }
    fun clearFilters() { _query.value = ""; _course.value = ""; _vis.value = "all"; _sort.value = "recent" }

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    init {
        loadUserInfo()
        getCourseCatalog()
        getPost()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            _username.value = dataStoreHelper.getFirstname() + " " + dataStoreHelper.getSurname()
        }
    }

    private val _wallRefresh = MutableStateFlow<WallRefreshState>(WallRefreshState.Idle)
    val wallRefresh: StateFlow<WallRefreshState> = _wallRefresh

    // =========================================================================================
    // ESTUDIANTE 4: rememberUpdatedState
    // Restablece el estado del banner a Idle cuando el temporizador de auto-ocultado se completa.
    // =========================================================================================
    fun dismissWallRefresh() {
        _wallRefresh.value = WallRefreshState.Idle
    }

    private var refreshJob: kotlinx.coroutines.Job? = null

    fun refresh(showFeedback: Boolean = false) = getPost(showFeedback)

    // Entrada exclusiva del regreso al muro; reintentar no genera anuncios de prueba.
    fun refreshAfterReturn() = getPost(showFeedback = true, simulateIncoming = true)

    private fun getPost(showFeedback: Boolean = false, simulateIncoming: Boolean = false) {
        if (showFeedback && refreshJob?.isActive == true) {
            Log.d("MuroDisposableEffect", "REFRESCO OMITIDO: ya hay una consulta en curso")
            return
        }
        refreshJob = viewModelScope.launch {
            if (showFeedback) {
                _wallRefresh.value = WallRefreshState.Loading
                Log.d("MuroDisposableEffect", "REFRESCO INICIADO: consultando anuncios")
            }
            val previousIds = _announcements.value.mapTo(mutableSetOf()) { it.id }
            var demoUpdate = false
            var simulatedPostId: Int? = null
            if (simulateIncoming &&
                com.erns.alertauni.data.demo.DemoConfig.ENABLED &&
                com.erns.alertauni.data.demo.DemoConfig.SIMULATE_INCOMING_POST_ON_RESUME &&
                repository is com.erns.alertauni.data.demo.FakePostRepository
            ) {
                // Latencia simulada para observar el indicador de carga durante la exposición.
                kotlinx.coroutines.delay(800)
                val incoming = repository.simulateIncomingPost()
                demoUpdate = true
                simulatedPostId = incoming.id
                Log.d("MuroDisposableEffect", "DEMO: anuncio de prueba ${incoming.id} incorporado al repositorio en memoria")
            }
            repository.getPosts().onSuccess { posts ->
                _announcements.value = posts
                if (simulatedPostId != null) _highlightedPostId.value = simulatedPostId
                if (showFeedback) {
                    val newPosts = posts.count { it.id !in previousIds }
                    _wallRefresh.value = WallRefreshState.Success(
                        completedAt = System.currentTimeMillis(),
                        newPosts = newPosts,
                        isDemo = demoUpdate
                    )
                    Log.d("MuroDisposableEffect", "CAMBIO VISIBLE: $newPosts anuncios nuevos; total ${posts.size}")
                    Log.d("MuroDisposableEffect", "REFRESCO COMPLETADO: ${posts.size} anuncios cargados")
                }
            }.onFailure { exception ->
                if (showFeedback) {
                    _wallRefresh.value = WallRefreshState.Error
                    Log.w("MuroDisposableEffect", "REFRESCO FALLIDO: no se pudieron actualizar los anuncios")
                }
                if (exception is BackendException) {
                    Log.d(
                        TAG,
                        "Error controlado: ${exception.errorData.code} -> ${exception.errorData.message}"
                    )
                } else {
                    Log.d(TAG, "Error común: ${exception.message}")
                }
            }

        }

    }

    fun sendPost(
        courseCatalogId: String,
        titlePost: String,
        contentPost: String,
        priority: String = "medium"
    ) {
        viewModelScope.launch {
            val postRequest = PostRequest(
                course_catalog_id = courseCatalogId,
                title = titlePost,
                content = contentPost,
                is_private = false,
                allow_comments = true,
                priority = priority
            )

            repository.sendPost(postRequest).onSuccess { postAddResponse ->
                _postState.value = PostState.Saved
                getPost()

            }.onFailure {
                Log.d(TAG, it.message.toString())
                _postState.value = PostState.UnSaved
            }

        }
    }

    private fun getCourseCatalog() {
        viewModelScope.launch {
            repository.getCourseCatalog().onSuccess { courses ->
                _catalogCourses.value = courses
            }.onFailure {
                Log.d(TAG, "Error loading course catalog: ${it.message}")
            }
        }

    }


}
