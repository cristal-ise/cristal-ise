/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.dev.test.dsl.module

import org.cristalise.dev.dsl.module.OpenApiParser
import org.junit.jupiter.api.Test

class OpenApiParserTest {

    @Test
    public void yaml() {
        def text = '''
openapi: 3.0.0
info:
  title: Car and Owner API
  version: 1.0.0
  description: A sample API for managing cars and their owners

components:
  schemas:
    Car:
      type: object
      properties:
        id:
          type: integer
          description: The unique identifier for the car
        make:
          type: string
          description: The make of the car
        model:
          type: string
          description: The model of the car
        year:
          type: integer
          description: The year the car was made
        owner:
          $ref: '#/components/schemas/Owner'

    Owner:
      type: object
      properties:
        id:
          type: integer
          description: The unique identifier for the owner
        name:
          type: string
          description: The name of the owner
        email:
          type: string
          format: email
          description: The email address of the owner
        cars:
          type: array
          items:
            $ref: '#/components/schemas/Car'
          description: The list of cars owned by the owner
'''
        def p = new OpenApiParser(name: 'test')
        
        def m = p.parse(text)
        
        println m.plantUml
    }

    @Test
    public void json() {
        def text = '''
{
  "openapi": "3.0.0",
  "info": {
    "title": "Car and Owner API",
    "version": "1.0.0"
  },
  "components": {
    "schemas": {
      "Car": {
        "type": "object",
        "properties": {
          "id": {
            "type": "integer"
          },
          "make": {
            "type": "string"
          },
          "model": {
            "type": "string"
          },
          "year": {
            "type": "integer"
          },
          "owner": {
            "$ref": "#/components/schemas/Car"
          }
        }
      },
      "Owner": {
        "type": "object",
        "properties": {
          "id": {
            "type": "integer"
          },
          "name": {
            "type": "string"
          },
          "email": {
            "type": "string"
          },
          "cars": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/Car"
            }
          }
        }
      }
    }
  }
}
'''
        def p = new OpenApiParser(name: 'test')
        
        def m = p.parse(text)
        
        println m.plantUml
    }
}
