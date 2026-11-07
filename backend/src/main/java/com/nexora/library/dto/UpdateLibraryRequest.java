package com.nexora.library.dto;

import com.nexora.library.LibraryStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateLibraryRequest {

    @NotNull(message = "Status is required")
    private LibraryStatus status;

    public UpdateLibraryRequest() { }

    public UpdateLibraryRequest(LibraryStatus status) {
        this.status = status;
    }

    public LibraryStatus getStatus() { return status; }
    public void setStatus(LibraryStatus status) { this.status = status; }
}
