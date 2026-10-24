package com.example.iot3.network;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.iot3.data.EnvironmentData;
import com.example.iot3.data.MonitorViewModel;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

/** Deterministic frontend lifecycle/failure tests; these sources never contact the cloud. */
public class CloudPollingTest {
    @Test
    public void onlyOneRequestRunsAndInactiveOwnerDoesNotPollAgain() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        CountDownLatch secondCall = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<MonitorViewModel> reference = new AtomicReference<>();
        ViewModelStore store = new ViewModelStore();
        Observer<String> observer = value -> {
            if ("云端数据已同步".equals(value)) {
                completed.countDown();
            }
        };
        try {
            main(() -> {
                MonitorViewModel model = new ViewModelProvider(store,
                        new ViewModelProvider.NewInstanceFactory()).get(MonitorViewModel.class);
                reference.set(model);
                model.refreshSeconds = 1;
                model.setSource(() -> {
                    if (calls.incrementAndGet() > 1) {
                        secondCall.countDown();
                    }
                    entered.countDown();
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IOException("test gate timeout");
                    }
                    return new EnvironmentData.Sample(1000, 22.9, 62.5, 472.5);
                });
                model.observeConnection().observeForever(observer);
                model.setActive(true);
                model.refreshFromCloud();
                assertTrue(model.isLoading());
                model.setActive(false);
            });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertEquals(1, calls.get());
            release.countDown();
            assertTrue(completed.await(5, TimeUnit.SECONDS));
            assertFalse(secondCall.await(1300, TimeUnit.MILLISECONDS));
            main(() -> {
                assertTrue(reference.get().hasReading());
                assertEquals(1, reference.get().current().history.size());
            });
        } finally {
            release.countDown();
            main(() -> {
                if (reference.get() != null) {
                    reference.get().observeConnection().removeObserver(observer);
                }
                store.clear();
            });
        }
    }

    @Test
    public void failedFetchPreservesLastRealReadingAndHistory() throws Exception {
        CountDownLatch failed = new CountDownLatch(1);
        ViewModelStore store = new ViewModelStore();
        AtomicReference<MonitorViewModel> reference = new AtomicReference<>();
        Observer<String> observer = value -> {
            if (value.startsWith("同步失败")) {
                failed.countDown();
            }
        };
        try {
            main(() -> {
                MonitorViewModel model = new ViewModelProvider(store,
                        new ViewModelProvider.NewInstanceFactory()).get(MonitorViewModel.class);
                reference.set(model);
                model.autoRefresh = false;
                model.updateReading(new EnvironmentData.Sample(1000, 22.9, 62.5, 472.5));
                model.setSource(() -> { throw new IOException("synthetic network failure"); });
                model.observeConnection().observeForever(observer);
                model.refreshFromCloud();
            });
            assertTrue(failed.await(5, TimeUnit.SECONDS));
            main(() -> {
                MonitorViewModel model = reference.get();
                assertFalse(model.isLoading());
                assertEquals("同步失败", model.connectionBadge());
                assertEquals(22.9, model.current().current.temperature, 0.001);
                assertEquals(1000, model.current().current.timestamp);
                assertEquals(1, model.current().history.size());
            });
        } finally {
            main(() -> {
                if (reference.get() != null) {
                    reference.get().observeConnection().removeObserver(observer);
                }
                store.clear();
            });
        }
    }

    private void main(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }
}
