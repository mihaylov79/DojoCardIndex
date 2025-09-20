output "mysql_flexible_server_fqdn" {
  description = "The FQDN of the MySQL Flexible Server"
  value       = azurerm_mysql_flexible_server.server.fqdn
}

output "mysql_database_name" {
  description = "The name of the database"
  value       = azurerm_mysql_flexible_database.db.name
}

output "mysql_admin_username" {
  description = "The administrator username for the MySQL server"
  value       = azurerm_mysql_flexible_server.server.administrator_login
}

output "mysql_connection_string" {
  description = "JDBC connection string"
  value       = "jdbc:mysql://${azurerm_mysql_flexible_server.server.fqdn}:3306/${azurerm_mysql_flexible_database.db.name}?useSSL=true&requireSSL=false&serverTimezone=UTC"
}
