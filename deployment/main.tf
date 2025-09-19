
terraform {
  required_providers {
    azurerm = {
      source = "hashicorp/azurerm"
      version = "4.45.0"
    }
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
  sku_name            = "F1"
}

resource "azurerm_virtual_network" "vn" {
  location            = azurerm_resource_group.rg.location
  name                = "drogon-dojo-vn"
  resource_group_name = azurerm_resource_group.rg.name
  address_space = ["10.0.0.0/16"]
}

resource "azurerm_subnet" "subnet" {
  address_prefixes = ["10.0.2.0/24"]
  name                 = "dragon-doje-subnet"
  resource_group_name  = azurerm_resource_group.rg.name
  virtual_network_name = azurerm_virtual_network.vn.name
}

resource "azurerm_mysql_flexible_server" "server" {
  location            = azurerm_resource_group.rg.location
  name                = "mysqlServer"
  resource_group_name = azurerm_resource_group.rg.name
  administrator_login = "root"
  administrator_password = var.admin_password
  sku_name = "Standard_B1ms"
  version = "8.0"

  high_availability {
    mode = "Disabled"
  }

  storage {
    size_gb = 16
  }

  backup_retention_days = 3

  delegated_subnet_id = azurerm_subnet.subnet.id
}

resource "azurerm_mysql_flexible_database" "db" {
  charset             = "utf8mb4"
  collation           = "utf8mb4_0900_ai_ci"
  name                = "dojo_DB"
  resource_group_name = azurerm_resource_group.rg.name
  server_name         = azurerm_mysql_flexible_server.server.name
}

