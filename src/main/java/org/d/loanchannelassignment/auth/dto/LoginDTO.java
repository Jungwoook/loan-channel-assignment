package org.d.loanchannelassignment.auth.dto;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginDTO {

    public class Request {
        String id;
        String pw;

    }

    public class Response {
        String id;
        String pw;

    }
}
