package org.eclipse.dataspaceconnector.catalog.cache.management;

import org.easymock.EasyMock;
import org.easymock.EasyMockExtension;
import org.easymock.Mock;
import org.easymock.MockType;
import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;
import static org.eclipse.dataspaceconnector.catalog.cache.TestUtil.createWorkItem;

/**
 * Unit test for the partition manager
 */
@ExtendWith(EasyMockExtension.class)
public class PartitionManagerImplTest {

    private PartitionManagerImpl partitionManager;
    @Mock(type = MockType.NICE)
    private Monitor monitorMock;
    @Mock(type = MockType.STRICT)
    private WorkItemQueue workItemQueueMock;
    private List<WorkItem> staticWorkload;

    @BeforeEach
    void setup() {
        staticWorkload = List.of(createWorkItem());
        partitionManager = new PartitionManagerImpl(monitorMock, workItemQueueMock, mockCrawler(), 5, () -> staticWorkload);
    }

    @Test
    @DisplayName("expect the workload to be put into the work item queue")
    void schedule() {
        workItemQueueMock.lock();
        expectLastCall();
        expect(workItemQueueMock.addAll(staticWorkload)).andReturn(true);
        workItemQueueMock.unlock();
        expectLastCall();
        replay(workItemQueueMock);

        partitionManager.schedule(Runnable::run);

        verify(workItemQueueMock);
    }

    @Test
    void stop_allCrawlersJoinSuccessfully() throws InterruptedException {
        List<Crawler> list = new ArrayList<>();
        var latch = new CountDownLatch(5);
        partitionManager = new PartitionManagerImpl(monitorMock, workItemQueueMock, workItems -> {
            Crawler crawler = strictMock(Crawler.class);
            crawler.run();
            expectLastCall().andAnswer(() -> {
                latch.countDown();
                return null;
            });
            expect(crawler.join()).andReturn(true);
            replay(crawler);
            list.add(crawler);
            return crawler;
        }, 5, () -> staticWorkload);


        // wait until all crawlers have run
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        partitionManager.stop();
        list.forEach(EasyMock::verify);
    }

    @NotNull
    private Function<WorkItemQueue, Crawler> mockCrawler() {
        return workItems -> {
            Crawler mock = niceMock(Crawler.class);
            replay(mock);
            return mock;
        };
    }
}
