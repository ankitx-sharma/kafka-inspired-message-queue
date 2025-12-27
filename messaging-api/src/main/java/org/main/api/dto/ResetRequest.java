package org.main.api.dto;

public record ResetRequest(
		boolean deleteDiskQueueFile
) {}