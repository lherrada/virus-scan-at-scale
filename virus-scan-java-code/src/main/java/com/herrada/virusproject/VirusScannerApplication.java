package com.herrada.virusproject;

import com.herrada.virusproject.Services.ScanService;
import com.herrada.virusproject.Services.TaskQueueService;
import com.herrada.virusproject.Listener.WorkerThread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
@EnableCaching
public class
VirusScannerApplication implements CommandLineRunner {
	public static void main(String[] args) throws  InterruptedException {
		SpringApplication.run(VirusScannerApplication.class, args);

		while (true)
		  Thread.sleep(500L);
    }

	@Autowired
	ScanService scanService;

	@Autowired
	TaskQueueService taskQueueService;

	@Override
	public void run(String... args) {
		ThreadFactory customThreadFactory = new ThreadFactory() {
			private final AtomicInteger counter = new AtomicInteger(1);
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r);
				thread.setName("Worker-" + counter.getAndIncrement());
				return thread;
			}
		};

		ExecutorService executor = Executors.newFixedThreadPool(3, customThreadFactory);
		Runnable runnable = new WorkerThread(taskQueueService, scanService);


		for (int i=0; i < 3; i++) {
			executor.execute(runnable);
		}
		executor.shutdown();
	}
}
