
terraform {
  required_providers {
    azurerm = {
      source = "hashicorp/azurerm"
      version = "4.45.0"
    }
  }

  backend "azurerm" {
    resource_group_name = "tfstate-rg"
    storage_account_name = "dragondojotfstate"
    container_name = "dojotfstate"
    key = "dragon-dojo.terraform.tfstate"
    # auth_mode            = "login"
  }
}

provider "azurerm" {
  features {}
  subscription_id = var.subscription_id
  tenant_id = var.tenant_id
}

resource "azurerm_resource_group" "rg" {
  location = "Switzerland North"
  name     = "dragon-dojo-rg"
}

resource "azurerm_service_plan" "sp" {
  location            = azurerm_resource_group.rg.location
  name                = "dragon-dojo-plan"
  os_type             = "Linux"
  resource_group_name = azurerm_resource_group.rg.name
  sku_name            = "B1"
}

# resource "azurerm_app_service_plan" "asp" {
#   location            = azurerm_resource_group.rg.location
#   name                = "dragon-dojo-plan"
#   resource_group_name = azurerm_resource_group.rg.name
#
#   kind = "Linux"
#   reserved = true
#
#   sku {
#     size = "B1"
#     tier = "Basic"
#   }
# }

# resource "azurerm_virtual_network" "vn" {
#   location            = azurerm_resource_group.rg.location
#   name                = "drogon-dojo-vn"
#   resource_group_name = azurerm_resource_group.rg.name
#   address_space = ["10.0.0.0/16"]
# }
#
# resource "azurerm_subnet" "subnet" {
#   address_prefixes = ["10.0.2.0/24"]
#   name                 = "dragon-doje-subnet"
#   resource_group_name  = azurerm_resource_group.rg.name
#   virtual_network_name = azurerm_virtual_network.vn.name
#
#   delegation {
#     name = "delegation"
#
#     service_delegation {
#       name    = "Microsoft.DBforMySQL/flexibleServers"
#       actions = ["Microsoft.Network/virtualNetworks/subnets/join/action", "Microsoft.Network/virtualNetworks/subnets/prepareNetworkPolicies/action"]
#     }
#   }
# }

resource "azurerm_mysql_flexible_server" "server" {
  location            = azurerm_resource_group.rg.location
  name                = "dragon-dojo-mysqlserver"
  resource_group_name = azurerm_resource_group.rg.name
  administrator_login = "dojoadmin"
  administrator_password = var.admin_password
  sku_name = "B_Standard_B1ms"
  version = "8.0.21"

  storage {
    size_gb = 20
  }

  backup_retention_days = 3

  # delegated_subnet_id = azurerm_subnet.subnet.id

}

resource "azurerm_mysql_flexible_database" "db" {
  charset             = "utf8mb4"
  collation           = "utf8mb4_0900_ai_ci"
  name                = "dojo_DB"
  resource_group_name = azurerm_resource_group.rg.name
  server_name         = azurerm_mysql_flexible_server.server.name
}

resource "azurerm_mysql_flexible_server_firewall_rule" "fr" {
  end_ip_address      = "255.255.255.255"
  name                = "dojo-firewall"
  resource_group_name = azurerm_resource_group.rg.name
  server_name         = azurerm_mysql_flexible_server.server.name
  start_ip_address    = "0.0.0.0"
}

# resource "azurerm_app_service" "app" {
#   app_service_plan_id = azurerm_app_service_plan.asp.id
#   location            = azurerm_resource_group.rg.location
#   name                = "dragon-dojo"
#   resource_group_name = azurerm_resource_group.rg.name
#
#   site_config {
#     linux_fx_version = "JAVA|17-java17"
#   }
#
#   app_settings = {
#     "SPRING_DATASOURCE_URL"      = "jdbc:mysql://mysqlServer.mysql.database.azure.com:3306/dojo_DB"
#     "SPRING_DATASOURCE_USERNAME" = "root"
#     "SPRING_DATASOURCE_PASSWORD" = var.admin_password
#   }
# }

resource "azurerm_linux_web_app" "alwa" {
  location            = azurerm_resource_group.rg.location
  name                = "dragon-dojo"
  resource_group_name = azurerm_resource_group.rg.name
  service_plan_id     = azurerm_service_plan.sp.id

  site_config {
    application_stack {
      java_version = "17"
      java_server = "JAVA"
      java_server_version = "17"
    }
  }

  app_settings = {

    # "SPRING_PROFILES_ACTIVE"   = "prod"
    # "SPRING_DATASOURCE_URL"    = "jdbc:mysql://mysqlserver.mysql.database.azure.com:3306/dojo_DB"
    # "SPRING_DATASOURCE_URL" = "jdbc:mysql://${azurerm_mysql_flexible_server.server.fqdn}:3306/${azurerm_mysql_flexible_database.db.name}"
    "SPRING_DATASOURCE_URL" = "jdbc:mysql://${azurerm_mysql_flexible_server.server.fqdn}:3306/${azurerm_mysql_flexible_database.db.name}?useSSL=true&requireSSL=false&serverTimezone=UTC"

    "SPRING_DATASOURCE_USERNAME" = "dojoadmin"
    "SPRING_DATASOURCE_PASSWORD" = var.admin_password
    "SPRING_PROFILES_ACTIVE"     = "prod"

  }
}

resource "azurerm_app_service_source_control" "vc" {
  app_id = azurerm_linux_web_app.alwa.id
  repo_url = "https://github.com/mihaylov79/DojoCardIndex"
  branch = "main"
  use_manual_integration = true
}

