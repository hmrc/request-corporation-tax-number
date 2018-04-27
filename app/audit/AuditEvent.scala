package audit

trait AuditEvent {
  def auditType: String
}