package de.isys.jawap.server

import de.isys.jawap.Application
import groovy.transform.AnnotationCollector
import org.junit.runner.RunWith
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.transaction.TransactionConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.transaction.annotation.Transactional


@RunWith(SpringJUnit4ClassRunner)
@WebAppConfiguration
@SpringApplicationConfiguration(classes = Application)
@TransactionConfiguration(defaultRollback = true)
@Transactional
@AnnotationCollector
public @interface WebAppJpaTest {

}