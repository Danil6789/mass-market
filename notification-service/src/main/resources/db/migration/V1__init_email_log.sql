CREATE TABLE email_log (
    id BIGSERIAL PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SENT','FAILED')),
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    error_message VARCHAR(1000)
);
CREATE INDEX idx_email_log_recipient ON email_log(recipient);
CREATE INDEX idx_email_log_status ON email_log(status);
