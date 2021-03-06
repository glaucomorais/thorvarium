package controllers

import models.User
import org.specs2.specification.Scope
import integration.WithTestDatabase
import org.specs2.execute.Results
import play.api.mvc.Controller
import play.api.test.{Helpers, FakeRequest, PlaySpecification}
import org.scalatest.mock.MockitoSugar
import session.SessionSpec
import session.SessionSpec.SessionTestComponentImpl

class UsersSpec extends PlaySpecification with Results with MockitoSugar with WithTestDatabase {

  class UsersTestController() extends Controller with Users with SessionTestComponentImpl

  "UsersTestController#login" should {
    "should return invalid_params in case of wrong parameters name" in {
      val controller = new UsersTestController()
      val result = controller.login().apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      bodyText must contain("invalid_params")
    }
    "should return invalid_params and cause in case of empty values" in {

      val controller = new UsersTestController()
      val request = FakeRequest(Helpers.POST, controllers.routes.Users.login().url)
          .withFormUrlEncodedBody(
            "nickname" -> "",
            "password" -> "")

      val result = call(controller.login, request)
      val bodyText: String = contentAsString(result)
      bodyText must contain("invalid_params")
    }
    "should return user_not_found in case of user not found" in new CreateUser {

      val controller = new UsersTestController()
      val request = FakeRequest(Helpers.POST, controllers.routes.Users.login().url)
          .withFormUrlEncodedBody(
            "nickname" -> "xxx",
            "password" -> "xxx")

      val result = call(controller.login, request)
      val bodyText: String = contentAsString(result)
      bodyText must contain("user_not_found")
    }
    "should return success and have uuid in case of user found" in new CreateUser {

      val controller = new UsersTestController()
      val request = FakeRequest(Helpers.POST, controllers.routes.Users.login().url)
          .withFormUrlEncodedBody(
            "nickname" -> "test",
            "password" -> "4297f44b13955235245b2497399d7a93")

      val result = call(controller.login, request)
      val bodyText: String = contentAsString(result)

      bodyText must contain("success")
      bodyText must contain(SessionSpec.testUUID)
    }
  }
  "UsersTestController#status" should {
    "should return invalid_params in case of wrong parameters name" in {
      val controller = new UsersTestController()
      val result = controller.status().apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      bodyText must contain("invalid_params")
    }
    "should return invalid_params and cause in case of empty values" in {

      val controller = new UsersTestController()
      val request = FakeRequest(Helpers.POST, controllers.routes.Users.status().url)
          .withFormUrlEncodedBody(
            "auth" -> "")

      val result = call(controller.status, request)
      val bodyText: String = contentAsString(result)
      bodyText must contain("invalid_params")
    }
    "should return unauthorized in case of auth not found" in new CreateUser {

      val controller = new UsersTestController()
      val request = FakeRequest(Helpers.POST, controllers.routes.Users.login().url)
          .withFormUrlEncodedBody(
            "nickname" -> "test",
            "password" -> "4297f44b13955235245b2497399d7a93")

      val result = call(controller.login, request)
      val bodyText: String = contentAsString(result)
      bodyText must contain("success")

      val requestStatus = FakeRequest(Helpers.POST, controllers.routes.Users.status().url)
          .withFormUrlEncodedBody(
            "auth" -> "xxx")

      val resultStatus = call(controller.status, requestStatus)
      val bodyStatusText: String = contentAsString(resultStatus)
      bodyStatusText must contain("unauthorized")
    }
    "should return success and have uuid in case of user found" in new CreateUser {

      val controller = new UsersTestController()
      val request = FakeRequest(Helpers.POST, controllers.routes.Users.login().url)
          .withFormUrlEncodedBody(
            "nickname" -> "test",
            "password" -> "4297f44b13955235245b2497399d7a93")

      val result = call(controller.login, request)
      val bodyText: String = contentAsString(result)
      bodyText must contain("success")
      bodyText must contain(SessionSpec.testUUID)

      val requestStatus = FakeRequest(Helpers.POST, controllers.routes.Users.status().url)
          .withFormUrlEncodedBody(
            "auth" -> SessionSpec.testUUID)

      val resultStatus = call(controller.status, requestStatus)
      val bodyStatusText: String = contentAsString(resultStatus)
      bodyStatusText must contain("success")
      bodyStatusText must contain("\"id\":1")
    }
    "should return unauthorized in case of user logout" in new CreateUser {

      val controller = new UsersTestController()
      val request = FakeRequest(Helpers.POST, controllers.routes.Users.login().url)
        .withFormUrlEncodedBody(
          "nickname" -> "test",
          "password" -> "4297f44b13955235245b2497399d7a93")

      val result = call(controller.login, request)

      val logoutRequest = FakeRequest(Helpers.POST, controllers.routes.Users.logout().url)
        .withFormUrlEncodedBody(
          "auth" -> SessionSpec.testUUID)

      val logoutStatus = call(controller.logout, logoutRequest)

      val bodyStatusText: String = contentAsString(logoutStatus)
      bodyStatusText must contain("success")
    }
  }

  class CreateUser extends Scope {
    val userID = User.save(User(Some(1), "test", "4297f44b13955235245b2497399d7a93"))
  }
}
