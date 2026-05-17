package com.demo.batch.application.port.out;
import com.demo.batch.application.dto.request.TriggerJobRequest;
/** Output port — abstracts Spring Batch job launching from the application layer. */
public interface BatchJobLauncherPort {
    String launchTransactionImportJob(TriggerJobRequest request) throws Exception;
}
