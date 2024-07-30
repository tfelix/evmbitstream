package de.tfelix.evmbitstream.upload

import de.tfelix.evmbitstream.download.DownloadController
import de.tfelix.evmbitstream.storage.StorageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder
import kotlin.reflect.jvm.javaMethod

@RestController
@RequestMapping("/v1/upload")
class UploadController(
    private val storageService: StorageService
) {

    @PostMapping
    fun handleFileUpload(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<Unit> {
        val hash = storageService.store(file)

        val fileUri = MvcUriComponentsBuilder
            .fromMethod(
                DownloadController::class.java,
                DownloadController::downloadFile.javaMethod!!,
                hash,
                null
            )
            .build()
            .toUri()

        return ResponseEntity.created(fileUri).build()
    }
}
