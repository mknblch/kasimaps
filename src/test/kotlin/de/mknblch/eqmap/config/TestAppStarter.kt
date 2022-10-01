package de.mknblch.eqmap.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootApplication
class TestAppStarter {

    @MockBean
    private lateinit var directoryWatcherService: DirectoryWatcherService

    @MockBean
    private lateinit var springFXMLLoader: SpringFXMLLoader

}